import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.io.PrettyAutomataReader;
import wyrw.core.*;

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
			Object[] nodes = new Object[automaton.nStates()];
			for (int i = 0; i != automaton.nStates(); ++i) {
				Automaton.State state = automaton.get(i);
				nodes[i] = graph.insertVertex(parent, null, schema.get(state.kind).name, 20, 20, 80, 30);
			}
			// Second, add all the children edges
			for (int i = 0; i != automaton.nStates(); ++i) {
				Automaton.State state = automaton.get(i);
				if(state instanceof Automaton.Collection) {
					Automaton.Collection col = (Automaton.Collection) state;
					for(int j = 0; j!=col.size();++j) {
						int child = col.get(j);
						graph.insertEdge(parent, null, "Edge", node[i],nodes[child]);						
					}
				}
			}

		}
		finally
		{
			graph.getModel().endUpdate();
		}

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		getContentPane().add(graphComponent);
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
		frame.draw(automaton);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 320);
		frame.setVisible(true);
	}

}