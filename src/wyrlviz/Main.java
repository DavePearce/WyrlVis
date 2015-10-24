package wyrlviz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
	
	/**
	 * The activation panel contains all the buttons for triggering an
	 * activation (i.e. a given rewrite on the automaton).
	 */
	private JPanel activationPanel;
	
	private JButton next;
	
	/**
	 * The main panel holds the automaton view along with the activation panel
	 */
	private JPanel mainPanel;
	
	/**
	 * The schema being used for the rewrite system
	 */
	private Schema schema;
	
	private Reduction rewrite;
	
	/**
	 * Current position within the rewrite being navigated
	 */
	private int HEAD;
	
	public Main(Schema schema)
	{
		super("WyRL Viewer");
		
		this.schema = schema;
		this.view = new AutomatonViewer(schema);
		this.activationPanel = createActivationPanel();
		this.mainPanel = createMainPanel();
		this.mainPanel.add(view,BorderLayout.CENTER);
		this.mainPanel.add(activationPanel,BorderLayout.EAST);
		getContentPane().add(mainPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		setMinimumSize(new Dimension(400,400));
		pack();
		setVisible(true);		
	}
	
	public void initialise(Automaton automaton, ReductionRule[] reductions) {
		// Finally, do the animation
		rewrite = new Reduction(schema,AbstractActivation.RANK_COMPARATOR,reductions);			
		HEAD = rewrite.initialise(automaton);
		draw(automaton);
	}
	
	public void next() {
		int next = rewrite.states().get(HEAD).select();
		if(next != -1) {
			HEAD = rewrite.step(HEAD, next);
			Rewrite.State state = rewrite.states().get(HEAD);
			draw(state.automaton());
		}
	}
	
	public void draw(Automaton automaton) {
		view.draw(automaton);
		view.repaint();
		view.revalidate();		
	}
	
	private JPanel createMainPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		return panel;
	}
	
	private JPanel createActivationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		next = new JButton(new AbstractAction("Next") {
			@Override
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});
		panel.add(next);
		return panel;
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
		frame.initialise(automaton, reductions);
	}

}