package com.interviewstreet.tweetdensity.servlet;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;

public class TweetDensity extends HttpServlet {
	private static final int MAXIMUM_COUNT = 200;
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		String handle = request.getParameter("handle");
		int count = Integer.parseInt(request.getParameter("count"));
		String type = request.getParameter("type");
		
		response.getWriter().println(processRequest(handle, count, type));
		response.getWriter().flush();
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		doGet(request, response);
	}
	
	// Process the request from the web call.
	public String processRequest(String handle, int count, String format) {
		try {
			long[] tweets = new long[24];
			long maximumID = 0;
		
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			while (count > 0) {			
				int requestCount;
				
				if (count > MAXIMUM_COUNT)
					requestCount = MAXIMUM_COUNT;
				else
					requestCount = count;
					
				count = count - MAXIMUM_COUNT;
			
				Document document = builder.parse(getURL(handle, requestCount, maximumID));			
				NodeList nodeList = document.getElementsByTagName("status");	

				int length = nodeList.getLength();
				
				for (int i = 0; i < length; i++) {
					Node node = nodeList.item(i);
					
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element)node;
						String createdAt = getTagValue("created_at", element);											
						int hour = Integer.parseInt(createdAt.substring(11, 13));
						
						tweets[hour] += 1;

						if (i == length - 1) {
							long id = Long.parseLong(getTagValue("id", element));
							maximumID = id - 1;
						}
					}
				}
			}
					
			if (format.equalsIgnoreCase("XML")) {
				return toXML(tweets);
			} else if (format.equalsIgnoreCase("JSON")) {
				return toJSON(tweets);
			} else {
				return toHTML(tweets);
			}
		} catch (Exception e) {
			return "";
		}
	}
	
	// Format the URL for the request to Twitter depending on the 
	// number of tweets to get and the maximum ID.
	protected String getURL(String handle, int count, long maximumID) {
		if (maximumID == 0)
			return String.format("https://api.twitter.com/1/statuses/user_timeline" + 
				".xml?include_entities=false&include_rts=1&" + 
				"screen_name=%s&count=%d", handle, count);
			
		return String.format("https://api.twitter.com/1/statuses/user_timeline" + 
			".xml?include_entities=false&include_rts=1&" + 
			"screen_name=%s&count=%d&max_id=%d", handle, count, maximumID);
	}
	
	// Get the value of the tag from the XML element.
	protected String getTagValue(String tag, Element element) {
		NodeList nodeList = element.getElementsByTagName(tag).
			item(0).getChildNodes();
		Node value = (Node)nodeList.item(0);
		
		return value.getNodeValue();
	}
	
	// Format the tweets to XML.
	protected String toXML(long[] tweets) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("<tweetdensity>\r\n");
		
		for (int i = 0; i < tweets.length; i++) {
			builder.append("\t<data>\r\n");
			builder.append(String.format("\t\t<hour>%02d</hour>\r\n", i));
			builder.append(String.format("\t\t<count>%d</count>\r\n", tweets[i]));
			builder.append("\t<data>\r\n");
		}
		
		builder.append("</tweetdensity>\r\n");
		
		return builder.toString();
	}
	
	// Format the tweets to JSON.
	protected String toJSON(long[] tweets) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("{\r\n\r\n");
		builder.append("\t\"tweetdensity\":{\r\n");
		builder.append("\t\t\"data\":[\r\n");
		
		for (int i = 0; i < tweets.length; i++) {
			builder.append("\t\t{\r\n");
			builder.append(String.format("\t\t\t\"hour\":%d,\r\n", i));
			builder.append(String.format("\t\t\t\"count\":%d\r\n", tweets[i]));
			
			if (i != tweets.length - 1)
				builder.append("\t\t},\r\n");
			else
				builder.append("\t\t}\r\n");
		}
		
		builder.append("\t\t]\r\n");
		builder.append("\t}\r\n");
		builder.append("}\r\n");
		
		return builder.toString();
	}
	
	// Format the tweets to HTML.
	protected String toHTML(long[] tweets) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("<html>\r\n");
		builder.append("\t<head>\r\n");
		builder.append("\t\t<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\r\n");
		builder.append("\t\t<script type=\"text/javascript\">\r\n");
		builder.append("\t\tgoogle.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\r\n");
		builder.append("\t\tgoogle.setOnLoadCallback(drawChart);\r\n");
		builder.append("\t\tfunction drawChart() {\r\n");
		builder.append("\t\t\tvar data = new google.visualization.DataTable();\r\n");
		builder.append("\t\t\tdata.addColumn(\"string\", \"Hour of day\");\r\n");
		builder.append("\t\t\tdata.addColumn(\"number\", \"Tweets\");\r\n");
		builder.append("\t\t\tdata.addRows([\r\n");
		
		for (int i = 0; i < tweets.length; i++) {
			if (i != tweets.length - 1)
				builder.append(String.format("\t\t\t\t[\"%d\", %d],\r\n", i, tweets[i]));
			else
				builder.append(String.format("\t\t\t\t[\"%d\", %d]\r\n", i, tweets[i]));
		}
		
		builder.append("\t\t\t]);\r\n");
		builder.append("\t\t\tvar options = {\r\n");
		builder.append("\t\t\t\twidth: \"100%\", height: 400, colors: [\"#FFD700\"],\r\n");
		builder.append("\t\t\t\tvAxis: {title: \"No. of tweets\", titleTextStyle: {color: \"#BEBEBE\"}, baselineColor: \"#BEBEBE\", format:\"#,###\"},\r\n");
		builder.append("\t\t\t\thAxis: {title: \"Hour of day\", titleTextStyle: {color: \"#BEBEBE\"}}\r\n");
		builder.append("\t\t\t};\r\n");
		builder.append("\t\t\tvar chart = new google.visualization.ColumnChart(document.getElementById(\"chart_div\"));\r\n");
		builder.append("\t\t\tchart.draw(data, options);\r\n");
		builder.append("\t\t}\r\n");
		builder.append("\t\t</script>\r\n");
		builder.append("\t</head>\r\n");
		builder.append("\t<body>\r\n");
		builder.append("\t\t<div id=\"chart_div\"></div>\r\n");
		builder.append("\t</body>\r\n");
		builder.append("</html>\r\n");
		
		return builder.toString();
	}
}
