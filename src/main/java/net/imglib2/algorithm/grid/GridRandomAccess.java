/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2015 Tobias Pietzsch, Stephan Preibisch, Barry DeZonia,
 * Stephan Saalfeld, Curtis Rueden, Albert Cardona, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Jonathan Hale, Lee Kamentsky, Larry Lindsey, Mark
 * Hiner, Michael Zinsmaier, Martin Horn, Grant Harris, Aivar Grislis, John
 * Bogovic, Steffen Jaensch, Stefan Helfrich, Jan Funke, Nick Perry, Mark Longair,
 * Melissa Linkert and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.algorithm.grid;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.view.Views;

/**
 * {@link RandomAccess} for a grid.
 * 
 * @author Simon Schmid (University of Konstanz)
 */
public class GridRandomAccess<T> extends Point implements RandomAccess<RandomAccessibleInterval<T>> {

	private RandomAccessibleInterval<T> srcImage;
	private long[] gap; // gap between two patches
	private long[] span; // span of the neighborhood of a patch -> size of patch
							// = 2 × span[d] + 1
	private int[] whichDims; // array contains zeros and ones and defines which
								// dimensions the patches have
	private long[] patchDims; // dimensions of the patch
	private long[] origin; // origin of the grid in the coordinates of the
							// source image
	private long[] gridDims; // dimensions of the grid

	// if parameters are added here, add them also in the copyRandomAccess()
	// method

	private GridRandomAccess(GridRandomAccess<T> gridRA) {
		super(new long[gridRA.whichDims.length]);
		this.origin = gridRA.origin.clone();
		this.gridDims = gridRA.gridDims.clone();
		this.gap = gridRA.gap.clone();
		this.span = gridRA.span.clone();
		this.whichDims = gridRA.whichDims.clone();
		this.patchDims = gridRA.patchDims.clone();
	}

	public GridRandomAccess(RandomAccessibleInterval<T> srcImage, long[] gap, long[] span, int[] whichDims,
			long[] origin, long[] gridDims) {
		super(new long[whichDims.length]);
		this.srcImage = srcImage;
		this.gap = gap;
		this.span = span;
		this.whichDims = whichDims;
		this.origin = origin;
		this.gridDims = gridDims;
		this.patchDims = new long[whichDims.length];
		for (int i = 0; i < whichDims.length; i++) {
			if (whichDims[i] == 0)
				patchDims[i] = 1;
			else
				patchDims[i] = span[i] * 2 + 1;
		}
	}

	@Override
	public Sampler<RandomAccessibleInterval<T>> copy() {
		return this.copy();
	}

	@Override
	public RandomAccessibleInterval<T> get() {
		// check if position is inside of the interval
		for (int i = 0; i < whichDims.length; i++) {
			if ((getLongPosition(i) < 0) || (getLongPosition(i) >= gridDims[i]))
				throw new IndexOutOfBoundsException("Position is out of bounds!");
		}

		// compute the actual position of the patch in the coordinates of the
		// source image
		long[] patchPos = new long[whichDims.length];
		for (int i = 0; i < whichDims.length; i++) {
			patchPos[i] = origin[i] + getLongPosition(i) * gap[i];
		}

		// compute min and max of the interval of the patch
		long[] min = new long[patchPos.length];
		long[] max = new long[patchPos.length];
		for (int i = 0; i < patchPos.length; i++) {
			if (whichDims[i] == 1) {
				min[i] = patchPos[i] - span[i];
				max[i] = patchPos[i] + span[i];
			} else {
				min[i] = patchPos[i];
				max[i] = patchPos[i];
			}
		}

		return Views.offsetInterval(srcImage, patchPos, patchDims);

	}

	@Override
	public RandomAccess<RandomAccessibleInterval<T>> copyRandomAccess() {
		return new GridRandomAccess<>(this);
	}

}
