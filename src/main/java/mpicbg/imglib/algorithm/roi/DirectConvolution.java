package mpicbg.imglib.algorithm.roi;


import java.util.Arrays;

import mpicbg.imglib.Localizable;
import mpicbg.imglib.img.Img;
import mpicbg.imglib.img.ImgFactory;
import mpicbg.imglib.img.ImgRandomAccess;
import mpicbg.imglib.img.array.ArrayImgFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsConstantValueFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsFactory;
import mpicbg.imglib.type.numeric.ComplexType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.util.Util;

/**
 * DirectConvolution is an ROIAlgorithm designed to do both convolution and cross-correlation 
 * by operating on the image and kernel directly, rather than by using such time-saving tricks as
 * FFT.
 * @author Larry Lindsey
 *
 * @param <T> input image type
 * @param <R> kernel type
 * @param <S> output image type
 */
public class DirectConvolution
	<T extends ComplexType<T>, R extends ComplexType<R>, S extends ComplexType<S>>
		extends ROIAlgorithm<T, S>
{

	protected static void quickKernel2D(short[][] vals, Img<ShortType> kern)
	{
		final ImgRandomAccess<ShortType> cursor = kern.randomAccess();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.get().set(vals[i][j]);
			}
		}	
	}
	
	public static Img<ShortType> sobelVertical()
	{
		final ImgFactory<ShortType> factory = new ArrayImgFactory<ShortType>();
		final Img<ShortType> sobel = factory.create(new long[]{3, 3}, new ShortType()); // "Vertical Sobel"
		final short[][] vals = {{-1, -2, -1},
				{0, 0, 0},
				{1, 2, 1}};
		
		quickKernel2D(vals, sobel);		
		
		return sobel;
	}
	
	public static Img<ShortType> sobelHorizontal()
	{
		final ImgFactory<ShortType> factory = new ArrayImgFactory<ShortType>();
		final Img<ShortType> sobel = factory.create(new long[]{3, 3}, new ShortType()); // "Horizontal Sobel"
		final short[][] vals = {{1, 0, -1},
				{2, 0, -2},
				{1, 0, -1}};
		
		quickKernel2D(vals, sobel);		
		
		return sobel;
	}
	
	private static long[] zeroArray(final int d)
	{
	    long[] zeros = new long[d];
	    Arrays.fill(zeros, 0);
	    return zeros;
	}
	
	private final Img<R> kernel;
	protected  final ImgRandomAccess<R> kernelCursor;
	
	private final S accum;
	private final S mul;
	private final S temp;
	
	public DirectConvolution(final S type, final Img<T> inputImage, final Img<R> kernel)
	{
		this(type, inputImage, kernel, new OutOfBoundsConstantValueFactory<T,Img<T>>(inputImage.firstElement().createVariable()));
	}
	
	// TODO ArrayImgFactory should use a type that extends NativeType
	public DirectConvolution(final S type, final Img<T> inputImage, final Img<R> kernel,
			final OutOfBoundsFactory<T,Img<T>> outsideFactory) {
		this(new ArrayImgFactory(), type, inputImage, kernel, outsideFactory);
	}
	
	public DirectConvolution(final ImgFactory<S> factory,
			final S type,
	        final Img<T> inputImage,
	        final Img<R> kernel,
			final OutOfBoundsFactory<T,Img<T>> outsideFactory)
	{
		super(factory, type.createVariable(),
				new StructuringElementCursor<T>(
						inputImage.randomAccess(outsideFactory), 
						Util.intervalDimensions(kernel),
						zeroArray(kernel.numDimensions())));

		getStrelCursor().centerKernel(Util.intervalDimensions(kernel));
		
		this.kernel = kernel;
		kernelCursor = kernel.randomAccess();
		
		//setName(inputImage.getName() + " * " + kernel.getName());
		
		accum = type.createVariable();
		mul = type.createVariable();
		temp = type.createVariable();
	}
		
	protected void setKernelCursorPosition(final Localizable l)
	{
	    kernelCursor.setPosition(l);
	}
	
	@Override
	protected boolean patchOperation(final StructuringElementCursor<T> strelCursor,
            final S outputType) {		
		T inType;
		R kernelType;
		
		accum.setZero();
			
		while(strelCursor.hasNext())
		{
		    
			mul.setOne();
			strelCursor.fwd();			
			setKernelCursorPosition(strelCursor);			
			
			inType = strelCursor.getType();
			kernelType = kernelCursor.get();
			
			temp.setReal(kernelType.getRealDouble());
			temp.setImaginary(-kernelType.getImaginaryDouble());			
			mul.mul(temp);
			
			temp.setReal(inType.getRealDouble());
			temp.setImaginary(inType.getImaginaryDouble());
			mul.mul(temp);
			
			accum.add(mul);			
		}
				
		outputType.set(accum);
		return true;
	}

	@Override
	public boolean checkInput() {
		if (super.checkInput())
		{
			// TODO there was a getOutputImage().getNumActiveCursors() instead
			if (kernel.numDimensions() == getOutputImage().numDimensions())
			{
				setErrorMessage("Kernel has different dimensionality than the Image");
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}

}