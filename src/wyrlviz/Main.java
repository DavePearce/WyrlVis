package wyrlviz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.io.PrettyAutomataReader;
import wyrl.util.Pair;
import wyrlviz.view.AutomatonViewer;
import wyrlviz.view.HistoryViewer;
import wyrw.core.*;
import wyrw.util.AbstractActivation;

public class Main extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2707712944901661771L;

	/**
	 * Responsible for displaying the automaton
	 */
	private AutomatonViewer view;
	
	/**
	 * Responsible for displaying the history graph, though might be null if not
	 * being used.
	 */
	private HistoryViewer hView;
	
	/**
	 * The menu
	 */
	private JMenuBar menubar;
	
	/**
	 * The activation panel contains all the buttons for triggering an
	 * activation (i.e. a given rewrite on the automaton).
	 */
	private JPanel activationPanel;
	
	private JPanel navigationPanel;
	
	private JButton first, back, next, last;
	
	private JLabel status;
	
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
	 * Current history of states visited in the order they are visited
	 */
	private ArrayList<Integer> history = new ArrayList<Integer>();
	
	/**
	 * Current position within the rewrite history
	 */
	private int hIndex;
	
	public Main(Schema schema, ReductionRule[] reductions)
	{
		super("WyRL Viewer");
		this.rewrite = new Reduction(schema,AbstractActivation.RANK_COMPARATOR,reductions);
		this.schema = schema;
		this.menubar = createMenuBar();
				
		this.view = new AutomatonViewer(schema);
		//view.setPreferredSize(new Dimension(300,390));
		this.activationPanel = createActivationPanel();
		this.navigationPanel = createNavigationPanel();
		this.mainPanel = createMainPanel();
		this.mainPanel.add(view,BorderLayout.CENTER);
		this.mainPanel.add(navigationPanel,BorderLayout.NORTH);
		this.mainPanel.add(activationPanel,BorderLayout.EAST);
		setJMenuBar(menubar);
		getContentPane().add(mainPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		setMinimumSize(new Dimension(400,400));
		pack();
		setVisible(true);		
	}
	
	public void initialise(Automaton automaton) {
		// Finally, do the animation
		int HEAD = rewrite.initialise(automaton);
		history.clear();
		history.add(HEAD);		
		hIndex = 0;
		reset();
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
			while(index >= history.size() && extendHistory()) {
				hIndex = hIndex + 1;
			}
		} else {
			hIndex = index;
		}
		// Second, check whether there actually is more history now
		reset();
	}
	
	public void menuSelected(String s) throws Exception {
		switch(s) {
		case "Open":
			openFile();
			closeViews();			
			break;
		case "Exit":
			exit();
		case "Show History":
			hView = new HistoryViewer(rewrite);
			break;
		default:
			throw new RuntimeException("Unknown menu item selected");
		}
	}
	
	public void closeViews() {
		if(hView != null) {
			hView.dispose();
		}
		hView = null;
	}
	
	/**
	 * Apply a given activation to the current HEAD.
	 * 
	 * @param activation
	 */
	public void apply(int activation) {
		if(extendHistory(activation)) {
			hIndex = history.size() - 1;
		} 
		reset();
	}
	
	public void draw(Automaton automaton) {
		view.draw(automaton);
		view.repaint();
		view.revalidate();		
	}
	
	private void reset() {
		int HEAD = history.get(hIndex);
		Rewrite.State state = rewrite.states().get(HEAD);
		draw(state.automaton());
		if(hView != null) { hView.update(history.get(hIndex)); }
		showActivations(state);
		if(hIndex > 0) {
			back.setEnabled(true);
			first.setEnabled(true);
		} else if(hIndex == 0) {
			first.setEnabled(false);
			back.setEnabled(false);
		}
		status.setText(Integer.toString(HEAD));
	}
	
	private boolean extendHistory() {
		int HEAD = history.get(hIndex);				
		int activation = rewrite.states().get(HEAD).select();
		return extendHistory(activation);
	}
	private boolean extendHistory(int activation) {
		int HEAD = history.get(hIndex);
		if(activation != -1) {
			Rewrite.State state = rewrite.states().get(HEAD);
			Rewrite.Step step = state.step(activation);
			int nHEAD;
			if(step != null) {
				// Activation previously taken
				nHEAD = step.after();
			} else {
				// Not previouslu taken
				nHEAD = rewrite.step(HEAD, activation);
				
			}
			if(nHEAD != HEAD) {			
				clearHistoryFrom(hIndex+1);
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
	
	private void showActivations(Rewrite.State state) {
		activationPanel.removeAll();		
		for(int i=0;i!=state.size();++i) {
			addActivationTrigger(i,state);			
		}
		this.revalidate();		
		this.repaint();
	}
	
	public void openFile() throws Exception {
		JFileChooser fileChooser = new JFileChooser(new File("."));
		File file;
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
		} else {
			// user cancelled open after all
			return;
		}
		// load file into rewrite viewer
		initialise(readAutomaton(file,schema));
	}

	public void exit() {
		// user is attempting to exit the interpreter.
		// make sure this is what they want to do
		// and check if changes need to be saved.
		int r = JOptionPane.showConfirmDialog(this, new JLabel(
				"Exit Interpreter?"), "Confirm Exit",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (r == JOptionPane.YES_OPTION) {
			// user still wants to go ahead.
			// if file not saved then prompt to check
			// whether it should be.
			System.exit(0);
		}
	}
	
	private void addActivationTrigger(final int activation, Rewrite.State state) {
		Rewrite.Step step = state.step(activation);
		String text = state.activation(activation).rule().name();
		// If the this activation has been previously taken, include the
		// destination state in the name.
		if(step != null) {
			text = text + " => #" + step.after();
		}
		// Create the activation trigger
		JButton trigger = new JButton(new AbstractAction(text) {
			@Override
			public void actionPerformed(ActionEvent e) {
				apply(activation);
			}
		});
		// If this activation is to the same state, then disable it
		if(step != null && step.after() == step.before()) {
			trigger.setEnabled(false);
		}
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = activation+1;		
		activationPanel.add(trigger,g);		
	}
	
	private JPanel createMainPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		return panel;
	}
	
	private JPanel createNavigationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		first = createNavButton("<<",Integer.MIN_VALUE);
		back = createNavButton("<",-1);			
		next = createNavButton(">",1);
		last = createNavButton(">>",Integer.MAX_VALUE);
		status = new JLabel("0");
		panel.add(first);
		panel.add(back);
		panel.add(status);
		panel.add(next);
		panel.add(last);
		return panel;
	}
	
	private JButton createNavButton(String text, final int jump) {
		return new JButton(new AbstractAction(text) {
			@Override
			public void actionPerformed(ActionEvent e) {
				jump(jump);
			}
		});
	}
	
	private JPanel createActivationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setPreferredSize(new Dimension(200,panel.getHeight()));
		return panel;
	}
	
	private JMenuBar createMenuBar() {
		// This function builds the menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File"); 
		fileMenu.add(makeMenuItem("Open"));
		fileMenu.addSeparator();
		fileMenu.add(makeMenuItem("Exit"));
		menuBar.add(fileMenu);
		// edit menu
		JMenu editMenu = new JMenu("Window");
		editMenu.add(makeMenuItem("Show History"));
		menuBar.add(editMenu);
		return menuBar;
	}
	
	private JMenuItem makeMenuItem(final String s) {
	    JMenuItem item = new JMenuItem(s);
	    item.setAction(new AbstractAction(s) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					menuSelected(s);
				} catch(Exception ex) {
					throw new RuntimeException("internal failure",ex);
				}
			}
		});
	    return item;
	}
	
	public static Automaton readAutomaton(File file, Schema schema) throws Exception {
		FileReader fr = new FileReader(file);
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
		
		// Second, construct the viewer
		Main frame = new Main(schema, reductions);		
	}

}