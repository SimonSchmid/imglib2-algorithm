package net.imglib2.algorithm.gauss2;

import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;

public class ConvertingSamplingLineIterator<A,B> extends AbstractSamplingLineIterator<B>
{
	final RandomAccess<A> randomAccess;
	final Converter<A, B> converter;
	final B temp;
	
	public ConvertingSamplingLineIterator( final int dim, final long size, final RandomAccess<A> randomAccess, final Converter<A, B> converter, final B temp )
	{
		super( dim, size, randomAccess );
		
		this.randomAccess = randomAccess;
		this.converter = converter;
		this.temp = temp;
	}

	@Override
	public B get()
	{
		converter.convert( randomAccess.get(), temp );
		return temp;
	}

	@Override
	public ConvertingSamplingLineIterator<A,B> copy()
	{
		// new instance with same properties
		ConvertingSamplingLineIterator<A, B> c = new ConvertingSamplingLineIterator<A, B>( d, sizeMinus1, randomAccess, converter, temp );
		
		// update current status
		c.i = i;
		
		return c;
	}
}
