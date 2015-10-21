import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.JFrame;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.io.PrettyAutomataReader;
import wyrl.util.Pair;
import wyrw.core.*;
import wyrw.util.AbstractActivation;

public class WyrlViz extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2707712944901661771L;

	/**
	 * The schema for the rewrite system we're visualising
	 */
	private Schema schema;
	
	public WyrlViz(Schema schema)
	{
		super("WyRL Viewer");

		this.schema = schema;
		
	}

	public void draw(Automaton automaton) {
		mxGraph graph = new mxGraph();
		Object parent = graph.getDefaultParent();

		graph.getModel().beginUpdate();
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

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		//mxFastOrganicLayout layoutifier = new mxFastOrganicLayout(graph);
		mxCompactTreeLayout layoutifier = new mxCompactTreeLayout(graph,false);
		//mxOrganicLayout layoutifier = new mxOrganicLayout(graph);
	    layoutifier.execute(parent);
		getContentPane().add(graphComponent);
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
	
	public static Automaton readAutomaton(String filename) throws Exception {
		FileReader fr = new FileReader(filename);
		PrettyAutomataReader reader = new PrettyAutomataReader(fr, Logic.SCHEMA);
		Automaton automaton = reader.read();
		fr.close();
		return automaton;
	}
	
	public static void main(String[] args) throws Exception
	{
		// First, load the automaton
		Automaton automaton = readAutomaton("test.aut");
		
		// Second, construct the viewer
		WyrlViz frame = new WyrlViz(Logic.SCHEMA);		

		
		// Finally, do the animation
		Reduction rewrite = new Reduction(Logic.SCHEMA,AbstractActivation.RANK_COMPARATOR,Logic.reductions);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 320);
		frame.setVisible(true);
		frame.draw(automaton);
		frame.repaint();
		frame.revalidate();
				
		int HEAD = rewrite.initialise(automaton);
		int next;
		while((next = rewrite.states().get(HEAD).select()) != -1) {
			try { Thread.sleep(1000); } catch(InterruptedException e) {};			
			HEAD = rewrite.step(HEAD, next);
			Rewrite.State state = rewrite.states().get(HEAD);
			frame.getContentPane().removeAll();
			frame.draw(state.automaton());	
			frame.repaint();
			frame.revalidate();				
		} 
	}

}