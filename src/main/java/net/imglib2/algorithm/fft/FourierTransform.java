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
 * An execption is the 1D FFT implementation of Dave Hale which we use as a
 * library, wich is released under the terms of the Common Public License -
 * v1.0, which is available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */
package net.imglib2.algorithm.fft;

import edu.mines.jtk.dsp.FftComplex;
import edu.mines.jtk.dsp.FftReal;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class FourierTransform<T extends RealType<T>, S extends ComplexType<S>> implements MultiThreaded, OutputAlgorithm<S>, Benchmark
{
	public static enum PreProcessing { NONE, EXTEND_MIRROR, EXTEND_MIRROR_FADING, USE_GIVEN_OUTOFBOUNDSSTRATEGY }
	public static enum Rearrangement { REARRANGE_QUADRANTS, UNCHANGED }
	public static enum FFTOptimization { SPEED, MEMORY }
	
	final RandomAccessible<T> input;
	final Interval interval;
	final int numDimensions;
	Img<S> fftImage;
	
	PreProcessing preProcessing;
	Rearrangement rearrangement;
	FFTOptimization fftOptimization;	
	float relativeImageExtensionRatio;
	int[] imageExtension;
	float relativeFadeOutDistance;
	int minExtension;
	int[] originalSize, originalOffset, extendedSize, extendedZeroPaddedSize;
	
	// if you want the image to be extended more use that
	int[] inputSize = null, inputSizeOffset = null;
	
	final S complexType;

	String errorMessage = "";
	int numThreads;
	long processingTime;

	public FourierTransform( final RandomAccessibleInterval<T> input, final S complexType, final PreProcessing preProcessing, final Rearrangement rearrangement,
							 final FFTOptimization fftOptimization, final float relativeImageExtension, final float relativeFadeOutDistance,
							 final int minExtension )
	{
		this.input = input;
		this.interval = input;
		this.complexType = complexType;
		this.numDimensions = input.numDimensions();
		this.extendedSize = new int[ numDimensions ];
		this.extendedZeroPaddedSize = new int[ numDimensions ];
		this.imageExtension = new int[ numDimensions ];
			
		//setPreProcessing( preProcessing );
		setRearrangement( rearrangement );
		setFFTOptimization( fftOptimization );
		setRelativeFadeOutDistance( relativeFadeOutDistance );
		setRelativeImageExtension( relativeImageExtension );
		setMinExtension( minExtension );

		this.originalSize = new int[ numDimensions ];
		this.originalOffset = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			if ( interval.dimension( d ) > Integer.MAX_VALUE - 1 )
				throw new RuntimeException( "FFT only supports a maximum size in each dimensions of " + (Integer.MAX_VALUE - 1) + ", but in dimension " + d + " it is " + interval.dimension( d ) );
			
			originalSize[ d ] = (int)interval.dimension( d );
		}
		
		this.processingTime = -1;		
		
		setNumThreads();
	}
	
	public FourierTransform( final RandomAccessibleInterval<T> input, final S complexType ) 
	{ 
		this ( input, complexType, /*PreProcessing.EXTEND_MIRROR_FADING, */Rearrangement.REARRANGE_QUADRANTS, 
		       FFTOptimization.SPEED, 0.25f, 0.25f, 12 ); 
	}

	public FourierTransform( final RandomAccessibleInterval<T> input, final S complexType, final Rearrangement rearrangement ) 
	{ 
		this ( input, complexType );
		setRearrangement( rearrangement );
	}

	public FourierTransform( final RandomAccessibleInterval<T> input, final S complexType, final FFTOptimization fftOptimization ) 
	{ 
		this ( input, complexType );
		setFFTOptimization( fftOptimization );
	}
	
	public FourierTransform( final RandomAccessibleInterval<T> input, final S complexType, final PreProcessing preProcessing ) 
	{ 
		this ( input, complexType );
		setPreProcessing( preProcessing );
	}
	
	public void setPreProcessing( final PreProcessing preProcessing ) { this.preProcessing = preProcessing; }
	public void setRearrangement( final Rearrangement rearrangement ) { this.rearrangement = rearrangement; }
	public void setFFTOptimization( final FFTOptimization fftOptimization ) { this.fftOptimization = fftOptimization; }
	public void setRelativeFadeOutDistance( final float relativeFadeOutDistance ) { this.relativeFadeOutDistance = relativeFadeOutDistance; }
	public void setMinExtension( final int minExtension ) { this.minExtension = minExtension; }	
	public void setImageExtension( final int[] imageExtension ) { this.imageExtension = imageExtension.clone(); }
	public boolean setExtendedOriginalImageSize( final int[] inputSize )
	{
		for ( int d = 0; d < numDimensions; ++d )
			if ( inputSize[ d ] < originalSize[ d ])
			{
				errorMessage = "Cannot set extended original image size smaller than image size";
				return false;
			}

		this.inputSize = inputSize.clone();
		this.inputSizeOffset = new int[ numDimensions ]; 
		
		setRelativeImageExtension( relativeImageExtensionRatio );
		
		return true;
	}
	
	public void setRelativeImageExtension( final float extensionRatio ) 
	{ 
		this.relativeImageExtensionRatio = extensionRatio;
		
		for ( int d = 0; d < interval.numDimensions(); ++d )
		{
			// how much do we want to extend
			if ( inputSize == null )
				imageExtension[ d ] = (int)Util.round( interval.dimension( d ) * ( 1 + extensionRatio ) ) - (int)interval.dimension( d );
			else
				imageExtension[ d ] = (int)Util.round( inputSize[ d ] * ( 1 + extensionRatio ) ) - (int)interval.dimension( d );
			
			if ( imageExtension[ d ] < minExtension )
				imageExtension[ d ] = minExtension;

			// add an even number so that both sides extend equally
			//if ( imageExtensionSum[ d ] % 2 != 0)
			//	++imageExtension[ d ];
						
			// the new size includes the current image size
			extendedSize[ d ] = imageExtension[ d ] + (int)interval.dimension( d );
		}			
	} 

	//public T getImageType() { return input.; }
	public int[] getExtendedSize() { return extendedSize.clone(); }	
	//public PreProcessing getPreProcessing() { return preProcessing; }
	public Rearrangement getRearrangement() { return rearrangement; }
	public FFTOptimization getFFOptimization() { return fftOptimization; }
	public float getRelativeImageExtension() { return relativeImageExtensionRatio; } 
	public int[] getImageExtension() { return imageExtension.clone(); }
	public float getRelativeFadeOutDistance() { return relativeFadeOutDistance; }
	//public OutOfBoundsStrategyFactory<T> getCustomOutOfBoundsStrategy() { return strategy; }
	public int getMinExtension() { return minExtension; }
	public int[] getOriginalSize() { return originalSize.clone(); }
	public int[] getOriginalOffset() { return originalOffset.clone(); }
	public int[] getFFTInputOffset( )
	{
		if ( inputSize == null )
			return originalOffset;
		else
			return inputSizeOffset;
	}
	public int[] getFFTInputSize( )
	{
		if ( inputSize == null )
			return originalSize.clone();
		else
			return inputSize.clone();
	}
	
	@Override
	public boolean process() 
	{		
		final long startTime = System.currentTimeMillis();

		//
		// perform FFT on the temporary image
		//			
		final OutOfBoundsStrategyFactory<T> outOfBoundsFactory;		
		switch ( preProcessing )
		{
			case USE_GIVEN_OUTOFBOUNDSSTRATEGY:
			{
				if ( strategy == null )
				{
					errorMessage = "Custom OutOfBoundsStrategyFactory is null, cannot use custom strategy";
					return false;
				}				
				extendedZeroPaddedSize = getZeroPaddingSize( getExtendedImageSize( img, imageExtension ), fftOptimization );
				outOfBoundsFactory = strategy;				
				break;
			}
			case EXTEND_MIRROR:
			{	
				extendedZeroPaddedSize = getZeroPaddingSize( getExtendedImageSize( img, imageExtension ), fftOptimization );
				outOfBoundsFactory = new OutOfBoundsStrategyMirrorFactory<T>();
				break;
				
			}			
			case EXTEND_MIRROR_FADING:
			{
				extendedZeroPaddedSize = getZeroPaddingSize( getExtendedImageSize( img, imageExtension ), fftOptimization );
				outOfBoundsFactory = new OutOfBoundsStrategyMirrorExpWindowingFactory<T>( relativeFadeOutDistance );				
				break;
			}			
			default: // or NONE
			{
				if ( inputSize == null )
					extendedZeroPaddedSize = getZeroPaddingSize( img.getDimensions(), fftOptimization );
				else
					extendedZeroPaddedSize = getZeroPaddingSize( inputSize, fftOptimization );
				
				outOfBoundsFactory = new OutOfBoundsStrategyValueFactory<T>( img.createType() );
				break;
			}		
		}
		
		originalOffset = new int[ numDimensions ];		
		for ( int d = 0; d < numDimensions; ++d )
		{
			if ( inputSize != null )
				inputSizeOffset[ d ] = ( extendedZeroPaddedSize[ d ] - inputSize[ d ] ) / 2;
			
			originalOffset[ d ] = ( extendedZeroPaddedSize[ d ] - img.getDimension( d ) ) / 2;			
		}
		
		
		fftImage = FFTFunctions.computeFFT( img, complexType, outOfBoundsFactory, originalOffset, extendedZeroPaddedSize, getNumThreads(), false );
		
		if ( fftImage == null )
		{
			errorMessage = "Could not compute the FFT transformation, most likely out of memory";
			return false;
		}

		// rearrange quadrants if wanted
		if ( rearrangement == Rearrangement.REARRANGE_QUADRANTS )
			FFTFunctions.rearrangeFFTQuadrants( fftImage, getNumThreads() );
			
        processingTime = System.currentTimeMillis() - startTime;

        return true;
	}	
				
	protected int[] getExtendedImageSize( final Image<?> img, final int[] imageExtension )
	{
		final int[] extendedSize = new int[ img.getNumDimensions() ];
		
		for ( int d = 0; d < img.getNumDimensions(); ++d )
		{
			// the new size includes the current image size
			extendedSize[ d ] = imageExtension[ d ] + img.getDimension( d );
		}
		
		return extendedSize;
	}
	
	protected int[] getZeroPaddingSize( final int[] imageSize, final FFTOptimization fftOptimization )
	{
		final int[] fftSize = new int[ imageSize.length ];
		
		// the first dimension is real to complex
		if ( fftOptimization == FFTOptimization.SPEED )
			fftSize[ 0 ] = FftReal.nfftFast( imageSize[ 0 ] );
		else
			fftSize[ 0 ] = FftReal.nfftSmall( imageSize[ 0 ] );
				
		// all the other dimensions complex to complex
		for ( int d = 1; d < fftSize.length; ++d )
		{
			if ( fftOptimization == FFTOptimization.SPEED )
				fftSize[ d ] = FftComplex.nfftFast( imageSize[ d ] );
			else
				fftSize[ d ] = FftComplex.nfftSmall( imageSize[ d ] );
		}
		
		return fftSize;
	}

	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	

	@Override
	public Image<S> getResult() { return fftImage; }

	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( img == null )
		{
			errorMessage = "Input image is null";
			return false;
		}
		else
		{
			return true;
		}
	}

	@Override
	public String getErrorMessage()  { return errorMessage; }
	
}
