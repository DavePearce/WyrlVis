package wyrlviz.view;

import java.awt.Dimension;
import java.util.HashSet;

import javax.swing.JPanel;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrl.util.Pair;

public class AutomatonViewer extends JPanel {
	/**
	 * The graph object used to represent the automaton
	 */
	private final mxGraph graph;
	
	/**
	 * The algorithm used to layout the automaton
	 */
	private final mxIGraphLayout layout;
	
	/**
	 * The schema used for the class of automata being displayed
	 */
	private final Schema schema;
	
	public AutomatonViewer(Schema schema) {
		this.schema = schema;
		this.graph = new mxGraph();
		this.layout = new mxCompactTreeLayout(graph,false);
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		add(graphComponent);					
	}
	
	/**
	 * Set the automaton which is to be drawn in the view. An effort is made 
	 * 
	 * @param automaton
	 */
	public void draw(Automaton automaton) {
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		graph.removeCells();
		try
		{
			// First, add all the nodes
			Pair<Integer,boolean[]> p = getVisibleStates(automaton);
			int zeroth = p.first();
			boolean[] visibleStates = p.second();
			Object[] nodes = new Object[visibleStates.length];			
			for (int i = 0; i !=visibleStates.length; ++i) {
				boolean vs = visibleStates[i];
				if(vs) {
					Automaton.State state = automaton.get(i-zeroth);
					String text;
					if(state instanceof Automaton.Constant) {
						Automaton.Constant c = (Automaton.Constant) state;
						text = c.toString();
					} else if(state instanceof Automaton.Set) {
						text = "{}";
					} else if(state instanceof Automaton.List) {
						text = "[]";
					} else {
						text = schema.get(state.kind).name;	
					}	
					nodes[i] = graph.insertVertex(parent, null, text, 20, 20, 40, 30, "ROUNDED");
				}
			}			
			// Second, add all the children edges
			for (int i = 0; i != automaton.nStates(); ++i) {
				Automaton.State state = automaton.get(i);
				if(state instanceof Automaton.Collection) {
					Automaton.Collection col = (Automaton.Collection) state;
					for(int j = 0; j!=col.size();++j) {
						int child = col.get(j);
						graph.insertEdge(parent, null, "", nodes[i+zeroth],nodes[child+zeroth]);						
					}
				} else if(state instanceof Automaton.Term) {
					Automaton.Term term = (Automaton.Term) state;
					if(term.contents != Automaton.K_VOID) {
						graph.insertEdge(parent, null, "", nodes[i+zeroth],nodes[term.contents+zeroth]);
					}
				}
			}

		}
		finally
		{
			graph.getModel().endUpdate();
		}

		//mxFastOrganicLayout layoutifier = new mxFastOrganicLayout(graph);
		mxCompactTreeLayout layoutifier = new mxCompactTreeLayout(graph,false);
		//mxOrganicLayout layoutifier = new mxOrganicLayout(graph);
	    layoutifier.execute(parent);
	}
	

	public static Pair<Integer,boolean[]> getVisibleStates(Automaton automaton) {
		HashSet<Integer> visited = new HashSet<Integer>();
		int min = 0;
		for(int i=0;i!=automaton.nRoots();++i) {
			min=Math.min(min,automaton.getRoot(i));
			visited.add(automaton.getRoot(i));
		}
		for (int i = 0; i != automaton.nStates(); ++i) {
			Automaton.State state = automaton.get(i);
			if (state instanceof Automaton.Collection) {
				Automaton.Collection c = (Automaton.Collection) state;
				for (int j = 0; j != c.size(); ++j) {
					int child = c.get(j);
					min = Math.min(min,child);
					visited.add(child);
				}
			} else if (state instanceof Automaton.Term) {
				Automaton.Term t = (Automaton.Term) state;
				if (t.contents != Automaton.K_VOID) {
					min = Math.min(min,t.contents);
					visited.add(t.contents);
				}
			}
		}
		boolean[] visible = new boolean[automaton.nStates()-min];
		for(int i=min;i!=automaton.nStates();++i) {
			visible[i-min] = visited.contains(i);
		}
		return new Pair<Integer,boolean[]>(-min,visible);
	}
}
