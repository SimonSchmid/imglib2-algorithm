package mpicbg.imglib.algorithm.gauss;

import mpicbg.imglib.algorithm.Precision.PrecisionReal;
import mpicbg.imglib.container.Img;
import mpicbg.imglib.container.ImgCursor;
import mpicbg.imglib.container.ImgRandomAccess;
import mpicbg.imglib.outofbounds.OutOfBoundsFactory;
import mpicbg.imglib.type.numeric.RealType;

public class GaussianConvolutionRealType<T extends RealType<T>> extends GaussianConvolution<T>
{
	protected PrecisionReal precision = PrecisionReal.Double;
	
	public GaussianConvolutionRealType( final Img<T> image, final OutOfBoundsFactory<T,Img<T>> outOfBoundsFactory, final double[] sigma )
	{
		super( image, outOfBoundsFactory, sigma );
		precision = image.firstElement().createVariable().getPreferredRealPrecision(); 
	}

	public GaussianConvolutionRealType( final Img<T> image, final OutOfBoundsFactory<T,Img<T>> outOfBoundsFactory, final double sigma )
	{
		super( image, outOfBoundsFactory, sigma );
		precision = image.firstElement().createVariable().getPreferredRealPrecision(); 
	}
	
	public void setPrecision( PrecisionReal precision ) { this.precision = precision; }
	public PrecisionReal getPrecision() { return precision; }
	
	@Override
	protected void convolve( final ImgRandomAccess<T> inputIterator, final ImgCursor<T> outputIterator, 
		 	   				 final int dim, final float[] kernel,
		 	   				 final long startPos, final long loopSize )
	{
		if ( precision == PrecisionReal.Float )
			convolveFloat( inputIterator, outputIterator, dim, kernel, startPos, loopSize );
		else
			convolveDouble( inputIterator, outputIterator, dim, kernel, startPos, loopSize );
	}
	
	protected void convolveDouble( final ImgRandomAccess<T> inputIterator, final ImgCursor<T> outputIterator, final int dim, final float[] kernel, final long startPos, final long loopSize )
	{		
		// move to the starting position of the current thread
		outputIterator.jumpFwd( startPos );
		
		final int filterSize = kernel.length;
		final int filterSizeMinus1 = filterSize - 1;
		final int filterSizeHalf = filterSize / 2;
		final int filterSizeHalfMinus1 = filterSizeHalf - 1;
		final int numDimensions = inputIterator.numDimensions();
		
		final int iteratorPosition = filterSizeHalf;
		
		final int[] to = new int[ numDimensions ];
		
		// do as many pixels as wanted by this thread
		for ( long j = 0; j < loopSize; ++j )
		{
			outputIterator.fwd();			                			                	
			
			// set the sum to zero
			double sum = 0, tmp = 0;
			
			//
			// we move filtersize/2 of the convolved pixel in the input image
			//
			
			// get the current positon in the output image
			outputIterator.localize( to );
			
			// position in the input image is filtersize/2 to the left
			to[ dim ] -= iteratorPosition;
			
			// set the input cursor to this very position
			inputIterator.setPosition( to );
			
			// iterate over the kernel length across the input image
			for ( int f = -filterSizeHalf; f <= filterSizeHalfMinus1; ++f )
			{
				// get value from the input image
				tmp = inputIterator.get().getRealDouble();
				
				// multiply the kernel
				tmp *= kernel[ f + filterSizeHalf ];
				
				// add up the sum
				sum += tmp;
				
				// move the cursor forward for the next iteration
				inputIterator.fwd( dim );
			}
			
			//
			// for the last pixel we do not move forward
			//
			
			// get value from the input image
			tmp = inputIterator.get().getRealDouble();
			
			// multiply the kernel
			tmp *= kernel[ filterSizeMinus1 ];
			
			// add up the sum
			sum += tmp;
			
			outputIterator.get().setReal( sum );			                		        	
		}
	}	

	protected void convolveFloat( final ImgRandomAccess<T> inputIterator, final ImgCursor<T> outputIterator, final int dim, final float[] kernel, final long startPos, final long loopSize )
	{		
		// move to the starting position of the current thread
		outputIterator.jumpFwd( startPos );
		
		final int filterSize = kernel.length;
		final int filterSizeMinus1 = filterSize - 1;
		final int filterSizeHalf = filterSize / 2;
		final int filterSizeHalfMinus1 = filterSizeHalf - 1;
		final int numDimensions = inputIterator.numDimensions();
		
		final int iteratorPosition = filterSizeHalf;
		
		final int[] to = new int[ numDimensions ];
		
		// do as many pixels as wanted by this thread
		for ( long j = 0; j < loopSize; ++j )
		{
			outputIterator.fwd();			                			                	
			
			// set the sum to zero
			float sum = 0, tmp = 0;
			
			//
			// we move filtersize/2 of the convolved pixel in the input image
			//
			
			// get the current positon in the output image
			outputIterator.localize( to );
			
			// position in the input image is filtersize/2 to the left
			to[ dim ] -= iteratorPosition;
			
			// set the input cursor to this very position
			inputIterator.setPosition( to );
			
			// iterate over the kernel length across the input image
			for ( int f = -filterSizeHalf; f <= filterSizeHalfMinus1; ++f )
			{
				// get value from the input image
				tmp = inputIterator.get().getRealFloat();
				
				// multiply the kernel
				tmp *= kernel[ f + filterSizeHalf ];
				
				// add up the sum
				sum += tmp;
				
				// move the cursor forward for the next iteration
				inputIterator.fwd( dim );
			}
			
			//
			// for the last pixel we do not move forward
			//
			
			// get value from the input image
			tmp = inputIterator.get().getRealFloat();
			
			// multiply the kernel
			tmp *= kernel[ filterSizeMinus1 ];
			
			// add up the sum
			sum += tmp;
			
			outputIterator.get().setReal( sum );			                		        	
		}
	}	
}
