/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Preibisch
 */
package mpicbg.imglib.algorithm.math;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.function.NormMinMax;
import mpicbg.imglib.container.Img;
import mpicbg.imglib.container.ImgCursor;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.util.RealSum;

public class NormalizeImageMinMax<T extends RealType<T>> implements Algorithm, MultiThreaded
{
	final Img<T> image;

	String errorMessage = "";
	int numThreads;
	
	public NormalizeImageMinMax( final Img<T> image )
	{
		setNumThreads();
		
		this.image = image;
	}
	
	@Override
	public boolean process()
	{
		final ComputeMinMax<T> minMax = new ComputeMinMax<T>( image );
		minMax.setNumThreads( getNumThreads() );
		
		if ( !minMax.checkInput() || !minMax.process() )
		{
			errorMessage = "Cannot compute min and max: " + minMax.getErrorMessage();
			return false;			
		}

		final double min = minMax.getMin().getRealDouble();
		final double max = minMax.getMax().getRealDouble();
		
		if ( min == max )
		{
			errorMessage = "Min and Max of the image are equal";
			return false;
		}		
		
		final ImageConverter<T, T> imgConv = new ImageConverter<T, T>( image, image, new NormMinMax<T>( min, max ) );
		imgConv.setNumThreads( getNumThreads() );
		
		if ( !imgConv.checkInput() || !imgConv.process() )
		{
			errorMessage = "Cannot divide by value: " + imgConv.getErrorMessage();
			return false;
		}
		
		return true;
	}

	public static <T extends RealType<T>> double sumImage( final Img<T> image )
	{
		final RealSum sum = new RealSum();
		final ImgCursor<T> cursor = image.cursor();
		
		while (cursor.hasNext())
		{
			cursor.fwd();
			sum.add( cursor.get().getRealDouble() );
		}
		
		return sum.getSum();
	}
	
	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "NormalizeImageReal: [Image<T> image] is null.";
			return false;
		}
		else
			return true;
	}

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	
	
	@Override
	public String getErrorMessage() { return errorMessage; }

}
