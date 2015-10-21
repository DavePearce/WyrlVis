package wyrlviz;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.JFrame;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.io.PrettyAutomataReader;
import wyrl.util.Pair;
import wyrlviz.view.AutomatonViewer;
import wyrw.core.*;
import wyrw.util.AbstractActivation;

public class Main extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2707712944901661771L;

	private AutomatonViewer view;
	
	public Main(Schema schema)
	{
		super("WyRL Viewer");

		this.view = new AutomatonViewer(schema);
		
		getContentPane().add(view);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400, 320);
		setVisible(true);
	}

	public void draw(Automaton automaton) {
		view.draw(automaton);
		view.repaint();
		view.revalidate();		
	}
	
	public static Automaton readAutomaton(String filename, Schema schema) throws Exception {
		FileReader fr = new FileReader(filename);
		PrettyAutomataReader reader = new PrettyAutomataReader(fr, schema);
		Automaton automaton = reader.read();
		fr.close();
		return automaton;
	}
	
	public static void main(String[] args) throws Exception
	{
		// First, decode rule set
		String ruleSetName = "Logic";//args[0];
		Class ruleSet = Class.forName(ruleSetName);
		Schema schema = (Schema) ruleSet.getField("SCHEMA").get(null);
		ReductionRule[] reductions = (ReductionRule[]) ruleSet.getField("reductions").get(null);
		InferenceRule[] inferenes = (InferenceRule[]) ruleSet.getField("inferences").get(null);
		
		
		// First, load the automaton
		Automaton automaton = readAutomaton("test.aut",schema);
		
		// Second, construct the viewer
		Main frame = new Main(schema);		

		
		// Finally, do the animation
		Reduction rewrite = new Reduction(schema,AbstractActivation.RANK_COMPARATOR,reductions);
		
		frame.draw(automaton);
				
		int HEAD = rewrite.initialise(automaton);
		int next;
		while((next = rewrite.states().get(HEAD).select()) != -1) {
			try { Thread.sleep(1000); } catch(InterruptedException e) {};			
			HEAD = rewrite.step(HEAD, next);
			Rewrite.State state = rewrite.states().get(HEAD);			
			frame.draw(state.automaton());				
		} 
	}

}