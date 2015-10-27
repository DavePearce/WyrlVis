package wyrlviz.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import wyrw.core.Rewrite;

public class HistoryViewer extends JFrame {
	/**
	 * The graph object used to represent the automaton
	 */
	private mxGraph graph;
	
	/**
	 * The swing interface
	 */
	private mxGraphComponent graphComponent; 
	
	/**
	 * The rewrite being viewed
	 */
	private final Rewrite rewrite;
	
	/**
	 * The set of all vertices currently being displayed
	 */
	private final ArrayList<Object> vertices = new ArrayList<Object>();
	
	/**
	 * The set of all vertices currently being displayed
	 */
	private final ArrayList<Object> edges = new ArrayList<Object>();
	
	
	public HistoryViewer(Rewrite rewrite) {
		this.rewrite = rewrite;
		this.graph = new mxGraph();
		this.graphComponent = new mxGraphComponent(graph);
		this.setPreferredSize(new Dimension(300,300));		
		getContentPane().add(graphComponent);
		pack();
		setVisible(true);
	}
	
	public void update(int active) {
		graphComponent.setPreferredSize(this.getSize());			
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();		
		// First, make sure there are enough cells!
		int oldSize = vertices.size();
		addMissingVertices(parent);
		// Second, add all new edges
		addMissingEdges(parent, oldSize);		
		// Third, highlight the active state
		highlightCell(active);
		// Done!
		graph.getModel().endUpdate();
		// Layout graph
		mxCompactTreeLayout layoutifier = new mxCompactTreeLayout(graph);
	    layoutifier.execute(parent);
	    graphComponent.repaint();
	}

	private void addMissingEdges(Object parent, int oldSize) {
		int index = edges.size();
		while (index < rewrite.steps().size()) {
			Rewrite.Step step = rewrite.steps().get(index++);
			if(step.before() != step.after()) {
				edges.add(graph.insertEdge(parent, null, "", vertices.get(step.before()), vertices.get(step.after())));
			} else {
				edges.add(null); // dummy
			}
		}
	}

	private void addMissingVertices(Object parent) {
		int index = vertices.size();
		while(index < rewrite.states().size()) {	
			String label = Integer.toString(index++);
			vertices.add(graph.insertVertex(parent, null, label, 20, 20, 20, 20, "ROUNDED"));
		}
	}

	private void highlightCell(int active) {
		for(int i=0;i!=vertices.size();++i) {
			if(i == active) {
				graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#CCCCCC", new Object[]{vertices.get(i)});
			} else {
				graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#EEEEEE", new Object[]{vertices.get(i)});
			}
		}
	}
}
