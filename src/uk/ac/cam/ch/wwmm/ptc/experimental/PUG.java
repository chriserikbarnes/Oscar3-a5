package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;

public class PUG {

	public static String chasePUG(Document responseDoc) throws Exception {
		Nodes resultNodes = responseDoc.query("//PCT-Download-URL_url");
		if(resultNodes.size() == 1) {
			return resultNodes.get(0).getValue();
		}
		Nodes waitingReqIdNodes = responseDoc.query("//PCT-Waiting_reqid");
		if(waitingReqIdNodes.size() == 1) {
			String reqId = waitingReqIdNodes.get(0).getValue();
			
			Element pugElem = new Element("PCT-Data");
			
			Element dataInput = new Element("PCT-Data_input");
			pugElem.appendChild(dataInput);
			
			Element inputData = new Element("PCT-InputData");
			dataInput.appendChild(inputData);
			
			Element inputDataRequest = new Element("PCT-InputData_request");
			inputData.appendChild(inputDataRequest);

			Element request = new Element("PCT-Request");
			inputDataRequest.appendChild(request);
	          //<PCT-Request_reqid>638302818484957496</PCT-Request_reqid>
	          //<PCT-Request_type value="status"/>
			
			Element requestReqid = new Element("PCT-Request_reqid");
			requestReqid.appendChild(reqId);
			request.appendChild(requestReqid);
			Element requestType = new Element("PCT-Request_type");
			requestType.addAttribute(new Attribute("value", "status"));
			request.appendChild(requestType);

			Document pugDoc = new Document(pugElem);

			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(pugDoc);
			
			URL url = new URL("http://pubchem.ncbi.nlm.nih.gov/pug/pug.cgi");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			Serializer ser2 = new Serializer(os);
			ser2.write(pugDoc);
			
			conn.connect();
			
			Document newResponseDoc = new Builder().build(conn.getInputStream());

			ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(newResponseDoc);

			
			chasePUG(newResponseDoc);
		}
		
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Element pugElem = new Element("PCT-Data");
		
		Element dataInput = new Element("PCT-Data_input");
		pugElem.appendChild(dataInput);
		
		Element inputData = new Element("PCT-InputData");
		dataInput.appendChild(inputData);
		
		Element inputDataDownload = new Element("PCT-InputData_download");
		inputData.appendChild(inputDataDownload);
		
		Element download = new Element("PCT-Download");
		inputDataDownload.appendChild(download);
		
		Element downloadUids = new Element("PCT-Download_uids");
		download.appendChild(downloadUids);
		
		Element queryUids = new Element("PCT-QueryUids");
		downloadUids.appendChild(queryUids);
		
		Element queryUidsIds = new Element("PCT-QueryUids_ids");
		queryUids.appendChild(queryUidsIds);
		
		Element idList = new Element("PCT-ID-List");
		queryUidsIds.appendChild(idList);
		
		Element idListDb = new Element("PCT-ID-List_db");
		idListDb.appendChild("pcsubstance");
		idList.appendChild(idListDb);
		
		Element idListUids = new Element("PCT-ID-List_uids");
		idList.appendChild(idListUids);
		
		Element idListUidsE = new Element("PCT-ID-List_uids_E");
		idListUidsE.appendChild("1");
		idListUids.appendChild(idListUidsE);
		idListUidsE = new Element("PCT-ID-List_uids_E");
		idListUidsE.appendChild("99");
		idListUids.appendChild(idListUidsE);
		
		Element downloadFormat = new Element("PCT-Download_format");
		downloadFormat.addAttribute(new Attribute("value", "sdf"));
		download.appendChild(downloadFormat);
		
		Element downloadCompression = new Element("PCT-Download_compression");
		downloadCompression.addAttribute(new Attribute("value", "gzip"));
		download.appendChild(downloadCompression);
		
		Document pugDoc = new Document(pugElem);
		Serializer ser = new Serializer(System.out);
		ser.setIndent(2);
		ser.write(pugDoc);
		
		URL url = new URL("http://pubchem.ncbi.nlm.nih.gov/pug/pug.cgi");
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		OutputStream os = conn.getOutputStream();
		Serializer ser2 = new Serializer(os);
		ser2.write(pugDoc);
		
		conn.connect();
		
		Document responseDoc = new Builder().build(conn.getInputStream());
		ser = new Serializer(System.out);
		ser.setIndent(2);
		ser.write(responseDoc);

		String urlStr = chasePUG(responseDoc);
		
		
	}

}
