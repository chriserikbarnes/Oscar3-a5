package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Provides a HTTP representation of the Oscar3 classpath/resource tree, to which you can
 * GET and PUT files.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ResourceServlet extends HttpServlet {
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		
		int lastSlash = path.lastIndexOf('/');
		String pathTo = path.substring(0, lastSlash+1);
		String name = path.substring(lastSlash+1);

		if(name.length() == 0 || !name.contains(".")) {
			response.setContentType("text/plain");
			response.getWriter().println("Error: can't PUT a directory");
			return;
		} 
		
		ResourceGetter rg = new ResourceGetter(pathTo);

		try {
			OutputStream out = rg.getOutputStream(name);
			InputStream in = request.getInputStream();
			for(int i=in.read();i!=-1;i=in.read()) out.write(i);
			out.close();
		} catch (Exception e) {
			response.setContentType("text/plain");
			response.getWriter().println("Error in PUT");			
			return;
		}
		
		doGet(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/*MultiPartRequest mpr = null;
		if(request.getContentType() != null &&
				request.getContentType().startsWith("multipart/form-data;")) mpr = new MultiPartRequest(request);
		System.out.println(mpr);
		String filename = mpr.getFilename("file");*/
		
		String filename = null;
		InputStream is = null;
						
		try	{
			if(request.getContentType() != null && 
					request.getContentType().startsWith("multipart/form-data;")) {
				FileItemFactory fif = new DiskFileItemFactory();
				ServletFileUpload sfu = new ServletFileUpload(fif);
				List itemList = sfu.parseRequest(request);
				for(Object o : itemList) {
					FileItem item = (FileItem)o;
					if(Oscar3Props.getInstance().verbose) System.out.println(item);
					if(item.getFieldName().equals("file")) {
						filename = item.getName();
						is = item.getInputStream();
						//if(true) return;
					}
				}
			}
			
			//if(true) return;

			
			ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/resources/");
			Document filePlaces = rg.getXMLDocument("filePlaces.xml");
			Elements ee = filePlaces.getRootElement().getChildElements();
			String place = null;
			String reload = null;
			for(int i=0;i<ee.size();i++) {
				Element e = ee.get(i);
				String pfn = e.getAttributeValue("name");
				if(Oscar3Props.getInstance().verbose) System.out.println(pfn);
				if((pfn.startsWith("*.") && filename.endsWith(pfn.substring(1))) || pfn.equals(filename)) {
					place = e.getAttributeValue("place");
					reload = e.getAttributeValue("reload");
					break;
				} 
			}
			if(place == null) {
				PrintWriter out = response.getWriter();
				out.println("I don't know where to put " + filename);
			} else {
				ResourceGetter nrg = new ResourceGetter(place);
				OutputStream os = nrg.getOutputStream(filename);
				assert(is != null);
				//InputStream is = null;
				//InputStream is = mpr.getInputStream("file");
				int c = is.read();
				while(c != -1) {
					os.write(c);
					c = is.read();
				}
				os.close();
				PrintWriter out = response.getWriter();
				out.println("Succesfully wrote: " + place + filename);
				if(reload != null && reload.length() > 0) {
					for(String toReload : StringTools.arrayToList(reload.split("\\s+"))) {
						Reload.reload(toReload, out);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String path = request.getPathInfo();
			
			int lastSlash = path.lastIndexOf('/');
			String pathTo = path.substring(0, lastSlash+1);
			String name = path.substring(lastSlash+1);
			
			if(Oscar3Props.getInstance().verbose) System.out.println(name);
			
			ResourceGetter rg = new ResourceGetter(pathTo);
			
			if(name.length() > 0 && !name.contains(".")) {
				if(Oscar3Props.getInstance().verbose) System.out.println("Foo!");
				if(Oscar3Props.getInstance().verbose) System.out.println(request.getServletPath());
				response.sendRedirect(request.getServletPath() + request.getPathInfo() + "/");
				return;
			}
			
			if(name.length() == 0) {
				PrintWriter out = response.getWriter();
				response.setContentType("text/html");
				out.write("<html><head><title>Oscar3 resources: " + path + "</title><head>");
				out.write("<body><p>Oscar3 resources: <a href='..'>" + path + "</a></p><ul>");
				for(String s : rg.getFiles()) {
					String maybeSlash = "";
					if(!s.contains(".")) {
						maybeSlash = "/";
					} else if(s.endsWith(".class") || s.equals("package.html") || s.equals("overview.html")) {
						continue;
					}
					out.write("<li><a href='" + s + maybeSlash + "'>" + s + "</a></li>");
				}
				out.write("</ul></body></html>");
				return;
			}
			
			if(name.endsWith(".txt")) {
				String out = rg.getString(name);				
				response.setContentType("text/plain");
				response.getWriter().println(out);				
			} else if(name.endsWith(".xml")) {
				String out = rg.getString(name);				
				response.setContentType("application/xml");
				response.getWriter().println(out);
			} else if(name.endsWith(".xml")) {
				String out = rg.getString(name);				
				response.setContentType("application/xslt+xml");
				response.getWriter().println(out);
			} else if(name.endsWith(".dtd")) {
				String out = rg.getString(name);				
				response.setContentType("application/xml-dtd");
				response.getWriter().println(out);
			} else {
				String out = rg.getString(name);				
				response.setContentType("text/plain");
				response.getWriter().println(out);								
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
