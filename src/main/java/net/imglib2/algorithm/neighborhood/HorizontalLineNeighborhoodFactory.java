package net.imglib2.algorithm.neighborhood;

import net.imglib2.RandomAccess;

public interface HorizontalLineNeighborhoodFactory< T >
{
	public Neighborhood< T > create( final long[] position, final long span, final int dim, final boolean skipCenter, final RandomAccess< T > sourceRandomAccess );
}
