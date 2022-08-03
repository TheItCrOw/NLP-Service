/**
 * Helper models for comment network
 * @author: Kevin
 */
package api.datamodels;

public class CommentLink_REST {
    private String source;
    private String target;
    private double sentiment;
    private int value;

    public String getSource(){return source;}
    public void setSource(String source) {this.source = source;}

    public String getTarget(){return target;}
    public void setTarget(String target){this.target = target;}

    public double getSentiment(){return sentiment;}
    public void setSentiment(double sentiment){this.sentiment = sentiment;}

    public int getValue(){return value;}
    public void setValue(int value){this.value = value;}
}
