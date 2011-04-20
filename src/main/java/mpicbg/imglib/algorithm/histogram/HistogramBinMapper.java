/**
 * Copyright (c) 2010, 2011 Larry Lindsey
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.  Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution.  Neither the name of the Fiji project nor
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
 *
 * @author Larry Lindsey
 */
package mpicbg.imglib.algorithm.histogram;

import mpicbg.imglib.type.Type;

/**
 * An interface used by the Histogram class to map Type objects to histogram
 * bins.
 */
public interface HistogramBinMapper <T>{
	
    /**
     * Returns the minimum bin for the histogram.  This value may not
     * be relevant for histograms over Type's that do not have a natural order.
     * @return the minimum bin Type for the histogram.
     */
	public T getMinBin();
	
    /**
     * Returns the maximum bin for the histogram.  This value may not
     * be relevant for histograms over Type's that do not have a natural order.
     * @return the maximum bin Type for the histogram.
     */
	public T getMaxBin();
	
	/**
	 * Returns the number of bins for the histogram.
	 * @return the number of bins for the histogram.
	 */
	public int getNumBins();

	/**
	 * Maps a given Type to its histogram bin.
	 * @param type the Type to map.
	 * @return the histogram bin index.
	 */
	public int map(final T type);
	
	/**
	 * Maps a given histogram bin index to a Type containing the bin center
	 * value.
	 * @param i the histogram bin index to map.
	 * @return a Type containing the bin center value.
	 */
	public T invMap(final int i);
}