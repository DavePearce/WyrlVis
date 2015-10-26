package wyrlviz.view;

import java.util.ArrayList;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Reduction;
import wyrw.core.ReductionRule;
import wyrw.core.Rewrite;
import wyrw.util.AbstractActivation;

public class NavigableReduction extends Reduction {

	/**
	 * Current history of states visited in the order they are visited
	 */
	private ArrayList<Integer> history = new ArrayList<Integer>();
	
	/**
	 * Current position within the rewrite history
	 */
	private int hIndex;
	
	public NavigableReduction(Schema schema, Comparator<AbstractActivation> comparator, ReductionRule[] reductions) {
		super(schema, comparator, reductions);
	}

	public int initialise(Automaton automaton) {
		int HEAD = super.initialise(automaton);
		history.clear();
		history.add(HEAD);		
		hIndex = 0;
		return HEAD;
	}
	
	public boolean atFront() {
		return hIndex == 0;
	}
	
	public Rewrite.State headState() {
		int HEAD = history.get(hIndex);
		return states.get(HEAD);
	}
	
	public int head() {
		return history.get(hIndex);
	}
	
	/**
	 * Jump to a relative position in the history. For example, +1 means advance
	 * 1 point.  If the delta is part the end of the history then we stop there.
	 * 
	 * @param delta
	 */
	public void jump(int delta) {
		int index = hIndex + delta;
		index = Math.max(0, index);		
		// First, check whether need to extend history or not
		if(index >= history.size()) {
			while(index >= history.size() && extend()) {
				hIndex = hIndex + 1;
			}
		} else {
			hIndex = index;
		}
	}
	
	public boolean extend() {
		int HEAD = history.get(hIndex);				
		int activation = states().get(HEAD).select();
		return extend(activation);
	}
	
	public boolean extend(int activation) {
		int HEAD = history.get(hIndex);
		if(activation != -1) {
			Rewrite.State state = states().get(HEAD);
			Rewrite.Step step = state.step(activation);
			int nHEAD;
			if(step != null) {
				// Activation previously taken
				nHEAD = step.after();
			} else {
				// Not previouslu taken
				nHEAD = step(HEAD, activation);
				
			}
			if(nHEAD != HEAD) {			
				clearHistoryFrom(++hIndex);
				history.add(nHEAD);
				return true;
			}
		}
		return false;
	}

	private void clearHistoryFrom(int index) {
		while(history.size() > index) {
			history.remove(history.size()-1);
		}
	}
	
}
