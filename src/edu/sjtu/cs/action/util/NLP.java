package edu.sjtu.cs.action.util;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class NLP {
	private static LexicalizedParser lp = LexicalizedParser.loadModel(
			"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
			"-maxLength", "80", "-retainTmpSubcategories");
	private static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	private static GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	
	public static String[] dependencyParse(String sentence){
		String[] result;
		String[] sent = sentence.split("((\\s+)|(?<=,)|(?=,)|(?=\\.))");
		Tree parse = lp.apply(Sentence.toWordList(sent));
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		result = new String[tdl.size()];
		int i = 0;
		for(TypedDependency t : tdl){
			result[ i++ ] = t.toString();
		}
		return result;
	}
	
}
