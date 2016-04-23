/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (c) 2009 - 2016, Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Philipp Hanslovsky, Grant Harris, Stefan Helfrich,
 * Mark Hiner, Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey,
 * Melissa Linkert, Mark Longair, Brian Northan, Nick Perry, Dimiter Prodanov,
 * Curtis Rueden, Johannes Schindelin, Jean-Yves Tinevez and Michael Zinsmaier.
 * All rights reserved.       
 *                            
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *                            
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *                            
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *                            
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.imglib2.algorithm.fill;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.type.Type;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import java.util.Comparator;


/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * Iterative n-dimensional flood fill for arbitrary neighborhoods.
 */
public class FloodFill {

    // int or long? current TLongList cannot store than Integer.MAX_VALUE
    private static final int CLEANUP_THRESHOLD = (int)1e5;


    /**
     * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting at seed location, write fillLabel
     * into target at current location and continue for each pixel in neighborhood defined by shape if neighborhood
     * pixel is in the same connected component and fillLabel has not been written into that location yet (comparator
     * evaluates to 0).
     *
     * Convenience call to {@link FloodFill#fill(RandomAccessible, RandomAccessible, Localizable, Comparable, Type, Shape)}.
     * seedLabel is extracted from source at seed location.
     *
     * @param source input
     * @param target {@link RandomAccessible} to be written into. May be the same as input.
     * @param seed Start flood fill at this location.
     * @param fillLabel Immutable. Value to be written into valid flood fill locations.
     * @param <T> T implements {@link Type<U>} and {@link Comparable<T>}.
     * @param <U> U implements {@link Type<U>} and {@link Comparable<U>}.
     */
    public static < T extends Type< T > & Comparable< T >, U extends Type< U > & Comparable< U > > void fill(
            final RandomAccessible< T > source,
            final RandomAccessible< U > target,
            final Localizable seed,
            final U fillLabel,
            final Shape shape
    )
    {
        RandomAccess<T> access = source.randomAccess();
        access.setPosition( seed );
        fill( source, target, seed, access.get().copy(), fillLabel, shape );
    }


    /**
     * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting at seed location, write fillLabel
     * into target at current location and continue for each pixel in neighborhood defined by shape if neighborhood
     * pixel is in the same connected component and fillLabel has not been written into that location yet (comparator
     * evaluates to 0).
     *
     * Convenience call to {@link FloodFill#fill(RandomAccessible, RandomAccessible, Localizable, Object, Object, Shape, Comparator, Writer)}
     * with {@link ComparableComparator} as comparator and {@link TypeWriter} as writer.
     *
     * @param source input
     * @param target {@link RandomAccessible} to be written into. May be the same as input.
     * @param seed Start flood fill at this location.
     * @param seedLabel Immutable. Reference value of input at seed location.
     * @param fillLabel Immutable. Value to be written into valid flood fill locations.
     * @param <T> T implements {@link Comparable<T>}.
     * @param <U> U implements {@link Type<U>} and {@link Comparable<U>}.
     */
    public static < T extends Comparable< T >, U extends Type< U > & Comparable< U > > void fill(
            final RandomAccessible< T > source,
            final RandomAccessible< U > target,
            final Localizable seed,
            final T seedLabel,
            final U fillLabel,
            final Shape shape
    )
    {
        fill( source, target, seed, seedLabel, fillLabel, shape, new ComparableComparator<T, U>(), new TypeWriter<U>() );
    }


    /**
     *
     * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting at seed location, write fillLabel
     * into target at current location and continue for each pixel in neighborhood defined by shape if neighborhood
     * pixel is in the same connected component and fillLabel has not been written into that location yet (comparator
     * evaluates to 0).
     *
     * @param source input
     * @param target {@link RandomAccessible} to be written into. May be the same as input.
     * @param seed Start flood fill at this location.
     * @param seedLabel Immutable. Reference value of input at seed location.
     * @param fillLabel Immutable. Value to be written into valid flood fill locations.
     * @param shape Defines neighborhood that is considered for connected components, e.g. {@link net.imglib2.algorithm.neighborhood.DiamondShape}
     * @param comparator Returns 0 if pixel has not been visited yet and should be written into. Returns non-zero if target pixel has been visited
     *                   or source pixel is not part of the same connected component. In the most trivial case, comparator will return 0
     *                   if and only if source at the current location is equal to seedLabel and target at the current location is different
     *                   from fillLabel, cf {@link ComparableComparator}.
     * @param writer Defines how fillLabel is written into target at current location.
     * @param <T> No restrictions on T. Appropriate comparator is the only requirement.
     * @param <U> No restrictions on U. Appropriate comparator and writer is the only requirement.
     */
    public static < T, U > void fill(
            final RandomAccessible< T > source,
            final RandomAccessible< U > target,
            final Localizable seed,
            final T seedLabel,
            final U fillLabel,
            final Shape shape,
            final Comparator< Pair< T, U > > comparator,
            final Writer< U > writer )
    {
        final int n = source.numDimensions();

        final ValuePair< T, U > reference = new ValuePair< T, U >( seedLabel, fillLabel );

        RandomAccessible< Pair< T, U > > paired = Views.pair(source, target);

        final TLongList[] coordinates = new TLongList[ n ];
        for ( int d = 0; d < n; ++d )
        {
            coordinates[ d ] = new TLongArrayList();
            coordinates[ d ].add( seed.getLongPosition( d ) );
        }

        final RandomAccessible<Neighborhood< Pair< T, U> > > neighborhood = shape.neighborhoodsRandomAccessible( paired );
        final RandomAccess<Neighborhood< Pair<T, U > > > neighborhoodAccess = neighborhood.randomAccess();

        final RandomAccess< U > targetAccess = target.randomAccess();
        targetAccess.setPosition( seed );
        writer.write( fillLabel, targetAccess.get() );

        for ( int i = 0; i < coordinates[ 0 ].size(); ++i )
        {
            for ( int d = 0; d < n; ++d )
                neighborhoodAccess.setPosition( coordinates[d].get(i), d);

            final Cursor< Pair< T, U > > neighborhoodCursor = neighborhoodAccess.get().cursor();

            while ( neighborhoodCursor.hasNext() )
            {
                Pair< T, U > p = neighborhoodCursor.next();
                if ( comparator.compare( p, reference ) == 0 )
                {
                    writer.write( fillLabel, p.getB() );
                    for ( int d = 0; d < n; ++d )
                        coordinates[ d ].add( neighborhoodCursor.getLongPosition( d ) );
                }
            }

            if ( i > CLEANUP_THRESHOLD )
            {
                for ( int d = 0; d < coordinates.length; ++d ) {
                    TLongList c = coordinates[ d ];
                    coordinates[ d ] = c.subList( i, c.size() );
                }
                i = 0;
            }

        }

    }

}
