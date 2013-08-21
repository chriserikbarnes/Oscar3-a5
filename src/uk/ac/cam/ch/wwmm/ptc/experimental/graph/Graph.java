package uk.ac.cam.ch.wwmm.ptc.experimental.graph;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Graph {

	/**An RDF-like graph library. Useful for simple experimentation without
	 * having all of the baggage of full-blown RDF. Will probably bridge
	 * nicely to RDF at some point.
	 * 
	 */
	static class GraphComparator implements Comparator<Graph> {
		public int compare(Graph g0, Graph g1) {
			// TODO Auto-generated method stub
			return new Integer(g0.allTriples.size()).compareTo(new Integer(g1.allTriples.size()));
		}
	}
	
	Set<Triple> allTriples;
	Map<String,Set<Triple>> triplesBySubject;
	Map<String,Set<Triple>> triplesByObject;
	Map<String,Set<Triple>> triplesByPredicate;
	
	Map<String,String> idToPrintable;
	
	public Graph() {
		allTriples = new HashSet<Triple>();
		triplesBySubject = new HashMap<String,Set<Triple>>();
		triplesByObject = new HashMap<String,Set<Triple>>();
		triplesByPredicate = new HashMap<String,Set<Triple>>();
		idToPrintable = new HashMap<String,String>();
	}

	public Graph(Set<Triple> triples, Graph original) {
		allTriples = new HashSet<Triple>();
		triplesBySubject = new HashMap<String,Set<Triple>>();
		triplesByObject = new HashMap<String,Set<Triple>>();
		triplesByPredicate = new HashMap<String,Set<Triple>>();
		idToPrintable = new HashMap<String,String>();
		
		for(Triple t : triples) {
			if(t.getSubject() != null && original.idToPrintable.containsKey(t.getSubject())) {
				idToPrintable.put(t.getSubject(), original.idToPrintable.get(t.getSubject()));
			}
			if(t.getObject() != null && original.idToPrintable.containsKey(t.getObject())) {
				idToPrintable.put(t.getObject(), original.idToPrintable.get(t.getObject()));
			}
			if(t.getPredictate() != null && original.idToPrintable.containsKey(t.getPredictate())) {
				idToPrintable.put(t.getPredictate(), original.idToPrintable.get(t.getPredictate()));
			}
			addTriple(t);
		}
	}
	
	public void addTriple(String subject, String predicate, String object) {
		Triple t = new Triple(subject, predicate, object);
		addTriple(t);
	}
	
	public void addTriple(Triple t) {
		allTriples.add(t);
		String subject = t.getSubject();
		if(subject != null) {
			if(!triplesBySubject.containsKey(subject)) triplesBySubject.put(subject, new HashSet<Triple>());
			triplesBySubject.get(subject).add(t);
		}
		String predicate = t.getPredictate();
		if(predicate != null) {
			if(!triplesByPredicate.containsKey(predicate)) triplesByPredicate.put(predicate, new HashSet<Triple>());
			triplesByPredicate.get(predicate).add(t);
		}
		String object = t.getObject();
		if(object != null) {
			if(!triplesByObject.containsKey(object)) triplesByObject.put(object, new HashSet<Triple>());
			triplesByObject.get(object).add(t);
		}
	}
	
	public void addPrintable(String id, String printable) {
		idToPrintable.put(id, printable);
	}
	
	public List<Graph> partitionToGraphs() {
		Set<Set<Triple>> graphs = partition();
		List<Graph> graphList = new ArrayList<Graph>();
		for(Set<Triple> tripleSet : graphs) graphList.add(new Graph(tripleSet, this));
		Collections.sort(graphList, Collections.reverseOrder(new GraphComparator()));
		return graphList;
	}
	
	public Set<Set<Triple>> partition() {
		Map<String,Set<Triple>> triplesByItem = new HashMap<String,Set<Triple>>();
		for(Triple t : allTriples) {
			if(t.getObject() != null && t.getSubject() != null) {
				if(!triplesByItem.containsKey(t.getSubject()) && !triplesByItem.containsKey(t.getObject())) {
					Set<Triple> tripleSet = new HashSet<Triple>();
					tripleSet.add(t);
					triplesByItem.put(t.getSubject(), tripleSet);
					triplesByItem.put(t.getObject(), tripleSet);
				} else if(triplesByItem.containsKey(t.getSubject())) {
					Set<Triple> tripleSet = triplesByItem.get(t.getSubject());
					tripleSet.add(t);
					if(!triplesByItem.containsKey(t.getObject())) {
						triplesByItem.put(t.getObject(), tripleSet);
					} else if(triplesByItem.get(t.getObject()).equals(tripleSet)) {
						// Do nothing
					} else {
						// Aha. We have to merge things
						Set<Triple> objectSet = triplesByItem.get(t.getObject());
						tripleSet.addAll(objectSet);
						for(Triple tt : objectSet) {
							if(tt.getSubject() != null) {
								triplesByItem.put(tt.getSubject(), tripleSet);
							}
							if(tt.getObject() != null) {
								triplesByItem.put(tt.getObject(), tripleSet);
							}
						}
					}
				} else {
					// Just the object
					Set<Triple> tripleSet = triplesByItem.get(t.getObject());
					tripleSet.add(t);
					triplesByItem.put(t.getSubject(), tripleSet);
				}
			} else if(t.getObject() == null && t.getSubject() == null) {
				// Do nothing, just a sentinel
			} else if(t.getObject() != null) {
				if(!triplesByItem.containsKey(t.getObject())) {
					triplesByItem.put(t.getObject(), new HashSet<Triple>());
				}
				triplesByItem.get(t.getObject()).add(t);
			} else if(t.getSubject() != null) {
				if(!triplesByItem.containsKey(t.getSubject())) {
					triplesByItem.put(t.getSubject(), new HashSet<Triple>());
				}
				triplesByItem.get(t.getSubject()).add(t);
			}
		}
		Set<Set<Triple>> results = new HashSet<Set<Triple>>(triplesByItem.values());
		return results;
	}
	
	public Set<Triple> getNeighbours(String item, int neighborhoodSize) {
		if(item == null) {
			return new HashSet<Triple>();
		} else if(neighborhoodSize == 1) {
			Set<Triple> triples = new HashSet<Triple>();
			if(triplesBySubject.containsKey(item)) triples.addAll(triplesBySubject.get(item));
			if(triplesByObject.containsKey(item)) triples.addAll(triplesByObject.get(item));
			return triples;
		} else if(neighborhoodSize > 1) {
			Set<Triple> triples = new HashSet<Triple>();
			if(triplesBySubject.containsKey(item)) {
				for(Triple t : triplesBySubject.get(item)) {
					if(t.getObject() != null) triples.addAll(getNeighbours(t.getObject(), neighborhoodSize-1));
					triples.add(t);
				}
			}
			if(triplesByObject.containsKey(item)) {
				for(Triple t : triplesByObject.get(item)) {
					if(t.getSubject() != null) triples.addAll(getNeighbours(t.getSubject(), neighborhoodSize-1));
					triples.add(t);
				}				
			}
			return triples;
		} else {
			throw new Error("Bad neighborhood size");
		}
	}
	
	public void rankNodes() {
		Bag<String> nodes = new Bag<String>();
		for(String subject : triplesBySubject.keySet()) {
			nodes.add(subject, triplesBySubject.get(subject).size());
		}
		for(String object : triplesByObject.keySet()) {
			nodes.add(object, triplesByObject.get(object).size());
		}
		System.out.println(idToPrintable);
		for(String node : nodes.getList()) {
			if(idToPrintable.containsKey(node)) {
				System.out.println(idToPrintable.get(node) + "\t" + nodes.getCount(node));
			} else {
				System.out.println(node + "\t" + nodes.getCount(node));				
			}
		}
	}
	
	public void removeNodes(Collection<String> nodes) {
		Set<Triple> toRemove = new HashSet<Triple>();
		for(String node : nodes) {
			if(triplesBySubject.containsKey(node)) toRemove.addAll(triplesBySubject.get(node));
			if(triplesByObject.containsKey(node)) toRemove.addAll(triplesByObject.get(node));
		}
		allTriples.removeAll(toRemove);
		for(String subject : new ArrayList<String>(triplesBySubject.keySet())) {
			if(nodes.contains(subject)) {
				triplesBySubject.remove(subject);
			} else {
				for(Triple t : new ArrayList<Triple>(triplesBySubject.get(subject))) {
					if(toRemove.contains(t)) triplesBySubject.get(subject).remove(t);
					if(triplesBySubject.get(subject).size() == 0) {
						triplesBySubject.remove(subject);
					}
				}				
			}
		}
		for(String object : new ArrayList<String>(triplesByObject.keySet())) {
			if(nodes.contains(object)) {
				triplesByObject.remove(object);
			} else {
				for(Triple t : new ArrayList<Triple>(triplesByObject.get(object))) {
					if(toRemove.contains(t)) triplesByObject.get(object).remove(t);
					if(triplesByObject.get(object).size() == 0) {
						triplesByObject.remove(object);
					}
				}				
			}
		}
		for(String predicate : new ArrayList<String>(triplesByPredicate.keySet())) {
			if(nodes.contains(predicate)) {
				triplesByPredicate.remove(predicate);
			} else {
				for(Triple t : new ArrayList<Triple>(triplesByPredicate.get(predicate))) {
					if(toRemove.contains(t)) triplesByPredicate.get(predicate).remove(t);
					if(triplesByPredicate.get(predicate).size() == 0) {
						triplesByPredicate.remove(predicate);
					}
				}				
			}
		}
	}
	
	private String getEdgeLabel(String predName) {
		if("is_a".equals(predName)) return "";
		return "[label=\"" + predName + "\"]";
	}
	
	public Bag<String> getNodes() {
		Bag<String> nodes = new Bag<String>();
		for(Triple t : allTriples) {
			if(t.getSubject() != null) nodes.add(t.getSubject());
			if(t.getObject() != null) nodes.add(t.getObject());
		}
		return nodes;
	}

	public String translate(String name) {
		if(idToPrintable.containsKey(name)) return idToPrintable.get(name);
		return name;
	}
	
	public void writeDot(PrintWriter pw) {
		pw.println("digraph G {");
		for(Triple t : allTriples) {
			String predName = t.getPredictate();
			if(idToPrintable.containsKey(predName)) predName = idToPrintable.get(predName);
			pw.println("\"" + t.getSubject() + "\" -> \"" + t.getObject() + "\" " + getEdgeLabel(predName));
		}
		Set<String> items = new HashSet<String>();
		items.addAll(triplesBySubject.keySet());
		items.addAll(triplesByObject.keySet());
		for(String item : items) {
			if(item.matches("CHEBI:\\d+")) {
				pw.println("\"" + item + "\" [URL=\"http://www.ebi.ac.uk/chebi/searchFreeText.do?searchString=" +
						StringTools.urlEncodeUTF8NoThrow(item) + "\"]");
			} else if(item.startsWith("InChI=")) {
				pw.print("\"" + item + "\" [URL=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?CMD=search&amp;db=pccompound&amp;term=%22" +
						StringTools.urlEncodeUTF8NoThrow(item) + "%22%5BInChI%5D\"]");
			}
			if(idToPrintable.containsKey(item)) pw.println("\"" + item + "\" [label=\"" + idToPrintable.get(item) + "\"]");
		}
		pw.println("}");
	}
	
	/*public static void main(String[] args) throws Exception {
		Graph g = new Graph();
		g.addTriple("foo", "bar", "baz");
		g.addTriple("foo", "bar", "baz");
		g.addTriple(null, "bar", "baz");
		g.addTriple("foo", "bar", "blork");
		g.addTriple("baz", "bar", "bean");
		g.addTriple("fish", "bar", "chips");
		
		g.addTriple("A", "->", "B");
		g.addTriple("B", "->", "C");
		g.addTriple("C", "->", "D");
		g.addTriple("D", "->", "E");
		g.addTriple("E", "->", "F");
		g.addTriple("F", "->", "G");
		g.addTriple("G", "->", "H");
		g.addTriple("H", "->", "I");
		g.addTriple("I", "->", "J");
		g.addTriple("J", "->", "K");
		g.addTriple("K", "->", "L");

		g.addTriple("G", "->", "B");
		System.out.println(g.partition());
		System.out.println(g.getNeighbours("F", 1));
		System.out.println(g.getNeighbours("F", 2));
		System.out.println(g.getNeighbours("F", 3));
		System.out.println(g.getNeighbours("Z", 3));

		Graph gg = new Graph(g.getNeighbours("G", 2), g);
		System.out.println(gg.allTriples);
		
		int i=0;
		for(Set<Triple> triples : g.partition()) {
			i++;
			gg = new Graph(triples, g);
			//File f = new File("/home/ptc24/tmp/newtest" + i + ".dot");
			PrintWriter pw = new PrintWriter(System.out);
			//pw.
			//PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			gg.writeDot(pw);
			//pw.close();
			pw.flush();
		}
	}*/

}
