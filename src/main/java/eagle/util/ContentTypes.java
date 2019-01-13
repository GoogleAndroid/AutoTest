package eagle.util;

import java.util.HashMap;
import java.util.Map;

public enum ContentTypes {
	FORM {
		@Override
		public String getContentType() {
			return "application/x-www-form-urlencoded";
		}
	},JSON {
		@Override
		public String getContentType() {
			return "application/json";
		}
	};
	public abstract String getContentType();
	public Map<String, String> contentType(){
		Map<String, String> maps=new HashMap<String,String>();
		ContentTypes[] hs=ContentTypes.class.getEnumConstants();
		for(ContentTypes h:hs){
			maps.put(h.toString(), h.getContentType());
		}
		return maps;
	}
}
