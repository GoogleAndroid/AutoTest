package eagle.dao;

import java.io.Serializable;

public class HttpClientResult implements Serializable {
    /**
     * status code
     */
    private  int code;
    /**
     * content
     */
    private  String content;
    public HttpClientResult(int code,String content){
        this.code=code;
        this.content=content;
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return  content;
    }
}

