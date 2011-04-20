package mpicbg.imglib.algorithm.roi;

import mpicbg.imglib.Localizable;
import mpicbg.imglib.img.Img;
import mpicbg.imglib.img.ImgFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.util.Util;

/**
 * DirectCrossCorr performs direct cross-correlation of a kernel against an image.
 * @author Larry Lindsey
 *
 * @param <T> input image type
 * @param <R> kernel type
 * @param <S> output image type 
 */
public class DirectCrossCorr
	<T extends RealType<T>, R extends RealType<R>, S extends RealType<S>>
		extends DirectConvolution<T, R, S>
{
    
    
    private final long[] kernelSizeMinusOne;
    private final long[] invertPos;
    
	public DirectCrossCorr(final S type, final Img<T> inputImage, final Img<R> kernel)
	{
		super(type, inputImage, kernel, null);
		//setName(inputImage + " x " + kernel.getName());
		kernelSizeMinusOne = Util.intervalDimensions(kernel);
		invertPos = new long[kernelSizeMinusOne.length];

		fixKernelSize();
	}
		
	public DirectCrossCorr(final S type, final Img<T> inputImage, final Img<R> kernel,
			final OutOfBoundsFactory<T,Img<T>> outsideFactory) {
		super(type, inputImage, kernel, outsideFactory);
		setName(inputImage + " x " + kernel);
		kernelSizeMinusOne = Util.intervalDimensions(kernel);
		invertPos = new long[kernelSizeMinusOne.length];

		fixKernelSize();
	}
	
	public DirectCrossCorr(final ImgFactory<S> factory,
			final S type,
            final Img<T> inputImage,
            final Img<R> kernel,
            final OutOfBoundsFactory<T,Img<T>> outsideFactory)
    {
	    super(factory, type, inputImage, kernel, outsideFactory);
	    //setName(inputImage + " x " + kernel);
	    kernelSizeMinusOne = Util.intervalDimensions(kernel);
	    invertPos = new long[kernelSizeMinusOne.length];
	    
	    fixKernelSize();
    }
	
	private void fixKernelSize()
	{
	    for (int i = 0; i < kernelSizeMinusOne.length; ++i)
	    {
	        kernelSizeMinusOne[i] -= 1;
	    }
	}
	
	protected void setKernelCursorPosition(final Localizable l)
    {
	    l.localize(invertPos);
	    for(int i = 0; i < invertPos.length; ++i)
	    {
	        invertPos[i] = kernelSizeMinusOne[i] - invertPos[i];
	    }
        kernelCursor.setPosition(invertPos);
    }

}