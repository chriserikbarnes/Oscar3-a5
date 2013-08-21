package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
/**Fetches abstracts from Pubmed and turns them into SciXML
 * 
 * @author ptc24
 *
 */
public final class PubmedToSciXML {
	
	/**Fetches abstracts from Pubmed, turns them into SciXML, and writes to the
	 * filesystem.
	 * 
	 * @param dir The directory to put the SciXML documents in.
	 * @param query The query to use.
	 * @param email An email address - the PubMed server needs one.
	 * @param abstracts The number of abstracts to fetch.
	 * @param skip The number of abstracts to skip.
	 * @throws Exception
	 */
	public static void queryToCorpus(File dir, String query, String email, int abstracts, int skip) throws Exception {
		Document pubmedResults = queryPubMed(query, email, abstracts, skip, false);
		if(pubmedResults == null) return;
		if(!dir.exists()) dir.mkdirs();
		if(!dir.isDirectory()) throw new Exception(dir.getAbsolutePath() + " is not a directory!");
		new Serializer(new FileOutputStream(new File(dir, "download.xml"))).write(pubmedResults);
		Map<String, SciXMLDocument> sciDocs = getSciXML(pubmedResults);
		for(String id : sciDocs.keySet()) {
			File subdir = new File(dir, id);
			subdir.mkdir();
			File sourceFile = new File(subdir, "source.xml");
			FileOutputStream fos = new FileOutputStream(sourceFile);
			new Serializer(fos).write(sciDocs.get(id));
			fos.close();
		}			
	}
	
	private static Map<String, SciXMLDocument> getSciXML(Document pubmedResults) throws Exception {
		XSLTransform xslt = new XSLTransform(new ResourceGetter("uk/ac/cam/ch/wwmm/ptclib/scixml/resources/").getXMLDocument("pubmed2scixml.xsl"));
		Map<String, SciXMLDocument> docs = new LinkedHashMap<String, SciXMLDocument>();
		Nodes n = pubmedResults.query("/PubmedArticleSet/PubmedArticle");
		for(int i=0;i<n.size();i++) {
			try {
				if(n.get(i).query(".//ArticleTitle").size() == 0) continue;
				if(n.get(i).query(".//AbstractText").size() == 0) continue;
				if(n.get(i).query(".//PMID").size() == 0) continue;
				String id = n.get(i).query(".//PMID").get(0).getValue();
				Element elem = (Element)n.get(i);
				elem.detach();
				Document d = new Document(elem);
				Document out = XSLTransform.toDocument(xslt.transform(d));
				docs.put(id, SciXMLDocument.makeFromDoc(out));
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Hmm, couldn't parse that abstract.");
			}
		}
		return docs;
	}
	
	/*public static Document queryPubMed(String query, String email, int abstracts, int skip) throws Exception {
		return queryPubMed(query, email, abstracts, skip, false);
	}*/
	
	private static Document queryPubMed(String query, String email, int abstracts, int skip, boolean useCache) throws Exception {
		Proxy proxy = null;
		if(useCache) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("wwwcache.cam.ac.uk", 8080));
		}

		String queryUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?";
		queryUrl += "db=pubmed&usehistory=y&retmode=xml";
		queryUrl += "&retmax=" + Integer.toString(abstracts);
		if(skip > 0 ) queryUrl += "&retstart=" + Integer.toString(skip);
		queryUrl +=	"&term=" + StringTools.urlEncodeUTF8NoThrow(query);
		queryUrl += "&tool=oscar3&email=";
		queryUrl += StringTools.urlEncodeUTF8NoThrow(email);
		if(Oscar3Props.getInstance().verbose) System.out.println(queryUrl);
		URL url = new URL(queryUrl);
		InputStream is;
		if(useCache) {
			is = url.openConnection(proxy).getInputStream();
		} else {
			is = url.openConnection().getInputStream();			
		}
		Document interDoc = new Builder(false).build(is);
		//new Serializer(System.out).write(interDoc);
		if(interDoc.query("//ErrorList").size() > 0) return null;
		String queryKey = interDoc.query("//QueryKey").get(0).getValue();
		String webEnv = interDoc.query("//WebEnv").get(0).getValue();
		queryUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
		queryUrl += "db=pubmed&retmode=xml";
		queryUrl += "&retmax=" + Integer.toString(abstracts);
		if(skip > 0 ) queryUrl += "&retstart=" + Integer.toString(skip);
		queryUrl += "&WebEnv=" + webEnv;
		queryUrl += "&query_key=" + queryKey;
		queryUrl += "&tool=oscar3&email=";
		queryUrl += StringTools.urlEncodeUTF8NoThrow("ptc24@cam.ac.uk");
		if(Oscar3Props.getInstance().verbose) System.out.println(queryUrl);
		url = new URL(queryUrl);
		Thread.sleep(1000);
		if(useCache) {
			is = url.openConnection(proxy).getInputStream();
		} else {
			is = url.openConnection().getInputStream();			
		}
		return new Builder(false).build(is);
	}
	
	/*public static void bulkDownload(String email, Calendar pmDate, Calendar startTime, Calendar endTime) throws Exception {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN);
		while(new GregorianCalendar().before(startTime)) {
			Thread.sleep(1000);
		}
		while(new GregorianCalendar().before(endTime)) {
			String dateStr = df.format(pmDate.getTime());
			System.out.println(dateStr + " at " + new Date());
			queryToCorpus(new File("/scratch/pubmed", dateStr), dateStr+"[EDAT]", email, 10000, 0);
			pmDate.add(Calendar.DATE, -1);
			Thread.sleep(3000);
		}
	}*/
	
	/*public static void main(String[] args) throws Exception {
		Document doc = new Builder().build(new File("/scratch/pubmed/2005/12/07/download.xml"));
		Nodes n = doc.query("/PubmedArticleSet/PubmedArticle[MedlineCitation/Article/Abstract/AbstractText]");
		XSLTransform xslt = new XSLTransform(new ResourceGetter("uk/ac/cam/ch/wwmm/scixml/resources/").getXMLDocument("pubmed2scixml.xsl"));
		for(int i=0;i<n.size();i++) {
			Element elem = (Element)n.get(i);
			elem.detach();
			Document d = new Document(elem);
			Document out = XSLTransform.toDocument(xslt.transform(d));
			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(out);
		}
		
		if(true) return;
		//queryToCorpus(new File("C:\\pubmedFiles"), "cancer", 20, 0);
		Calendar gCal = new GregorianCalendar(2005, 11, 31);
		//DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN);
		//gCal.add(Calendar.DATE, -5);
		
		Calendar startTime = new GregorianCalendar();
		startTime.add(Calendar.DATE, 1);
		startTime.set(Calendar.HOUR_OF_DAY, 2);
		startTime.set(Calendar.MINUTE, 0);
		startTime.set(Calendar.SECOND, 0);
		Calendar endTime = (Calendar)startTime.clone();
		endTime.set(Calendar.MINUTE, 55);
		endTime.set(Calendar.HOUR_OF_DAY, 9);
		System.out.println(startTime.getTime());
		System.out.println(endTime.getTime());
		bulkDownload(gCal, startTime, endTime);
		
		//long time = System.currentTimeMillis();
		//queryToCorpus(new File("/home/ptc24/pmtest3"), df.format(gCal.getTime())+"[EDAT]", 100, 0);
		//System.out.println(System.currentTimeMillis() - time);
	}*/

}
