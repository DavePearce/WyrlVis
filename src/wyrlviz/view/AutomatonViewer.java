package wyrlviz.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashSet;

import javax.swing.JPanel;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrl.util.Pair;

public class AutomatonViewer extends JPanel {
	private static final String CONSTANT_STYLE = "#e3daab";
	private static final String SET_STYLE = "#9edbe1";
	private static final String LIST_STYLE = "#8ac4c9";
	private static final String TERM_STYLE = "#9acb8d";
	private static final String HIGHLIGHT_STYLE = "#EEEEEE";
	/**
	 * The graph object used to represent the automaton
	 */
	private mxGraph graph;
	
	/**
	 * The swing interface
	 */
	private mxGraphComponent graphComponent; 
	
	/**
	 * The algorithm used to layout the automaton
	 */
	private final mxIGraphLayout layout;
	
	private Automaton automaton;
	
	private int zeroth;
	
	private Object[] nodes;
	
	public AutomatonViewer() {
		this.graph = new mxGraph();
		this.layout = new mxCompactTreeLayout(graph,false);
		this.graphComponent = new mxGraphComponent(graph);
		add(graphComponent);
		// Intercept resize events
		this.addComponentListener(new ComponentListener() {

    		public void componentResized(ComponentEvent e) {
    			updateInternalDimensions();
    		}

    		public void componentHidden(ComponentEvent e) {}

    		public void componentMoved(ComponentEvent e) {}

    		public void componentShown(ComponentEvent e) {}
    	});

	}
	
	public void setHighlight(int state, boolean on) {
		String style = getFillStyle(state,on); 
		Object vertex = nodes[state+zeroth];
		graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, style, new Object[]{vertex});
	}
	
	/**
	 * Set the automaton which is to be drawn in the view. An effort is made 
	 * 
	 * @param automaton
	 */
	public void draw(Automaton aut, Schema schema) {
		// THIS IS UGLY
		automaton = aut;
		graph = new mxGraph();
		graphComponent.setGraph(graph);	
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		graph.removeCells();
		try
		{
			// First, add all the nodes
			Pair<Integer,boolean[]> p = getVisibleStates(automaton);
			zeroth = p.first();
			boolean[] visibleStates = p.second();
			nodes = new Object[visibleStates.length];			
			for (int i = 0; i !=visibleStates.length; ++i) {
				boolean vs = visibleStates[i];
				if(vs) {
					Automaton.State state = automaton.get(i-zeroth);
					String text;
					String style;
					if(state instanceof Automaton.Constant) {
						Automaton.Constant c = (Automaton.Constant) state;
						text = c.toString();
						style = CONSTANT_STYLE;
					} else if(state instanceof Automaton.Set) {
						text = "{}";
						style = SET_STYLE;
					} else if(state instanceof Automaton.List) {
						text = "[]";
						style = LIST_STYLE;
					} else {
						text = schema.get(state.kind).name;
						style = TERM_STYLE;
					}	
					nodes[i] = graph.insertVertex(parent, null, text, 20, 20, 40, 30, "ROUNDED");
					graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, style, new Object[]{nodes[i]});
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

		mxCompactTreeLayout layoutifier = new mxCompactTreeLayout(graph,false);
	    layoutifier.execute(parent);
	    // Center image
	    mxRectangle r = graph.getView().getGraphBounds();
	    int xoff = this.getWidth()/2 - ((int)r.getWidth()/2);
	    int yoff = this.getHeight()/2 - ((int)r.getHeight()/2);
	    mxPoint p = new mxPoint(xoff,yoff);
	    graph.getView().setTranslate(p);
	}
	
	public String getFillStyle(int stateID, boolean highlight) {
		Automaton.State state = automaton.get(stateID);
		if(highlight) {
			return HIGHLIGHT_STYLE;
		} else if(state instanceof Automaton.Constant) {
			return CONSTANT_STYLE;
		} else if(state instanceof Automaton.Set) {
			return SET_STYLE;
		} else if(state instanceof Automaton.List) {
			return LIST_STYLE;
		} else {
			return TERM_STYLE;
		}	
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
	
	private void updateInternalDimensions() {
		Insets insets = this.getInsets();
		
		int width = getWidth() - (insets.left + insets.right);
		int height = getHeight() - (insets.top + insets.bottom);
		Dimension dim = new Dimension(width,height);
		graphComponent.setPreferredSize(dim);
	    mxRectangle r = graph.getView().getGraphBounds();
	    int xoff = this.getWidth()/2 - ((int)r.getWidth()/2);
	    int yoff = this.getHeight()/2 - ((int)r.getHeight()/2);
	    mxPoint p = new mxPoint(xoff,yoff);
	    graph.getView().setTranslate(p);

	}
}
