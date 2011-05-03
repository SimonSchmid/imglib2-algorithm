/**
 * Copyright (c) 2011, Tobias Pietzsch & Stephan Preibisch & Stephan Saalfeld
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.  Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution.  Neither the name of the imglib project nor
 * the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
package net.imglib2.algorithm.math;

import net.imglib2.Localizable;
import net.imglib2.util.Util;

/**
 * Provides an instance of {@link Localizable} that can store positions
 * 
 * @author Stephan Preibisch
 */
public class LocalizablePoint implements Localizable 
{
	final long[] position;
	final int numDimensions;

	public LocalizablePoint ( final Localizable position )
	{
		this( position.numDimensions() );

		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = position.getLongPosition( d );
	}

	public LocalizablePoint ( final long[] position )
	{
		this( position.length );

		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = position[ d ];
	}

	public LocalizablePoint ( final int[] position )
	{
		this( position.length );

		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = position[ d ];
	}

	public LocalizablePoint ( final float[] position )
	{
		this( position.length );
		
		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = Util.round( position[ d ] );
	}

	public LocalizablePoint ( final int numDimensions )
	{
		this.numDimensions = numDimensions;
		this.position = new long[ numDimensions ];
	}
	
	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = this.position[ d ];
	}

	@Override
	public void localize( final double[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = this.position[ d ];
	}

	@Override
	public float getFloatPosition( final int d ) { return position[ d ]; }

	@Override
	public double getDoublePosition( final int d ) { return position[ d ]; }

	@Override
	public int numDimensions() { return numDimensions;	}

	@Override
	public void localize( final int[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = (int)this.position[ d ];
	}

	@Override
	public void localize( final long[] position )
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = this.position[ d ];
	}

	@Override
	public int getIntPosition( final int d ) { return (int)position[ d ]; }

	@Override
	public long getLongPosition( final int d )  { return position[ d ]; }
}
