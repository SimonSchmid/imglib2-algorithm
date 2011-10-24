package net.imglib2.algorithm.mser;

import java.util.ArrayList;

import net.imglib2.Localizable;
import net.imglib2.type.numeric.IntegerType;

public final class SimpleMserEvaluationNode< T extends IntegerType< T > >
{
	public static final boolean verbose = true;

	/**
	 * Threshold value of the connected component.
	 */
	public final long value;

	/**
	 * Size (number of pixels) of the connected component.
	 */
	public final long size;

	public final ArrayList< SimpleMserEvaluationNode< T > > ancestors;
	public final SimpleMserEvaluationNode< T > historyAncestor;
	public SimpleMserEvaluationNode< T > successor;
	
	/**
	 * MSER score : |Q_{i+\Delta} - Q_i| / |Q_i|. 
	 */
	public double score;
	public boolean isScoreValid;

	//for verbose output:
	public final ArrayList< Localizable > locations;
	public final int componentId;

	public SimpleMserEvaluationNode( final SimpleMserComponent< T > component, final long delta, final SimpleMserComponentHandler.SimpleMserProcessor< T > minimaProcessor )
	{
		value = component.getValue().getIntegerLong();
		size = component.getSize();

		ancestors = new ArrayList< SimpleMserEvaluationNode< T > >();
		SimpleMserEvaluationNode< T > n = component.getEvaluationNode();
		long historySize = 0;
		if ( n != null )
		{
			historySize = n.size;
			n = createIntermediateNodes( component.getEvaluationNode(), value, delta, minimaProcessor );
			ancestors.add( n );
			n.setSuccessor( this );
		}

		SimpleMserEvaluationNode< T > historyWinner = n;
		for ( SimpleMserComponent< T > c : component.getAncestors() )
		{
			n = createIntermediateNodes( c.getEvaluationNode(), component.getValue().getIntegerLong(), delta, minimaProcessor );
			ancestors.add( n );
			n.setSuccessor( this );
			if ( c.getSize() > historySize )
			{
				historyWinner = n;
				historySize = c.getSize();
			}
		}
		
		historyAncestor = historyWinner;
		
		if ( verbose )
		{
			locations = getLocationsFromComponent( component );
			componentId = component.id;
		}
		else
		{
			locations = null;
			componentId = 0;
		}
		
		component.setEvaluationNode( this );
		isScoreValid = computeMserScore( delta );
		if ( isScoreValid )
			for ( SimpleMserEvaluationNode< T > a : ancestors )
				a.evaluateLocalMinimum( minimaProcessor );
	}

	private SimpleMserEvaluationNode( final SimpleMserEvaluationNode< T > ancestor, final long value, final long delta, final SimpleMserComponentHandler.SimpleMserProcessor< T > minimaProcessor )
	{
		ancestors = new ArrayList< SimpleMserEvaluationNode< T > >();
		ancestors.add( ancestor );
		ancestor.setSuccessor( this );

		historyAncestor = ancestor;
		size = ancestor.size;
		this.value = value;

		if ( verbose )
		{
			locations = ancestor.locations;
			componentId = ancestor.componentId;
		}
		else
		{
			locations = null;
			componentId = 0;
		}

		isScoreValid = computeMserScore( delta );
		if ( isScoreValid )
			ancestor.evaluateLocalMinimum( minimaProcessor );
	}

	@SuppressWarnings( "unchecked" )
	private ArrayList< Localizable > getLocationsFromComponent( final SimpleMserComponent< T > component )
	{
		return ( ArrayList< Localizable > ) component.locations.clone();
	}

	private SimpleMserEvaluationNode< T > createIntermediateNodes( final SimpleMserEvaluationNode< T > fromNode, final long toValue, final long delta, final SimpleMserComponentHandler.SimpleMserProcessor< T > minimaProcessor )
	{
		SimpleMserEvaluationNode< T > n = fromNode;
		for ( long v = n.value + 1; v < toValue; ++v )
			n = new SimpleMserEvaluationNode< T >( n, v, delta, minimaProcessor );
		return n;
	}

	private void setSuccessor( SimpleMserEvaluationNode< T > n )
	{
		successor = n;
	}

	/**
	 * Evaluate the mser score at this connected component.
	 * This may fail if the connected component tree is not built far enough
	 * down from the current node.
	 */
	private boolean computeMserScore( final long delta )
	{
		// we are looking for a precursor node with value == (this.value - delta)
		final long valueMinus = value - delta;
		// go back in history until we find a node with (value == valueMinus)
		SimpleMserEvaluationNode< T > n = historyAncestor;
		while ( n != null  &&  n.value > valueMinus )
			n = n.historyAncestor;
		if ( n == null )
			// we cannot compute the mser score because the history is too short.
			return false;
		score = ( size - n.size ) / ( ( double ) size );
		return true;		
	}

	/**
	 * Check whether the mser score is a local minimum at this connected
	 * component. This may fail if the mser score for this component, or the
	 * previous one in the branch are not available. (Note, that this is only
	 * called, when the mser score for the next component in the branch is
	 * available.)
	 */
	private void evaluateLocalMinimum( final SimpleMserComponentHandler.SimpleMserProcessor< T > minimaProcessor )
	{
		if ( isScoreValid && historyAncestor.isScoreValid )
			if ( ( score <= historyAncestor.score ) && ( score < successor.score ) )
				minimaProcessor.foundNewMinimum( this );
	}

	@Override
	public String toString()
	{
		if ( verbose )
		{
			String s = "SimpleMserEvaluationNode constructed from component " + componentId;
			s += ", size=" + size;
			s += ", history = [";
			SimpleMserEvaluationNode< T > n = historyAncestor;
			boolean first = true;
			while ( n != null )
			{
				if ( first )
					first = false;
				else
					s += ", ";	
				s += "(" + n.value + "; " + n.size;
				if ( n.isScoreValid )
					s += " s " + n.score + ")";
				else
					s += " s --)";
				n = n.historyAncestor;
			}
			s += "]";
			return s;
		}
		else
			return super.toString();
	}
}
