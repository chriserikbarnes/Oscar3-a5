package uk.ac.cam.ch.wwmm.ptc.experimental;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import opennlp.maxent.Context;
import opennlp.maxent.GISModel;

public class Tamperer {
	
	public static GISModel tamperModel(GISModel model, Set<String> featuresToRemove, List<String> prefixesToRemove) {
		Object [] modelData = model.getDataStructures();
		TObjectIntHashMap map = (TObjectIntHashMap)modelData[1];
		Context [] params = (Context []) modelData[0];
		//map.
		List<Context> newParams = new ArrayList<Context>();
		List<String> newPredNames = new ArrayList<String>();
		
		TObjectIntIterator iterator = map.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String predName = (String)iterator.key();
			int index = map.get(predName);
			Context context = params[index];
			boolean goodPredName = true;
			if(featuresToRemove != null && featuresToRemove.contains(predName)) goodPredName = false;
			for(String prefix : prefixesToRemove) {
				if(predName.startsWith(prefix)) goodPredName = false;
			}
			if(goodPredName) {
				newPredNames.add(predName);
				newParams.add(context);
			} 
		}
		return new GISModel(newParams.toArray(new Context[0]), newPredNames.toArray(new String[0]), (String [])modelData[2], (Integer)modelData[3], (Double)modelData[4]);
	}
	
}
