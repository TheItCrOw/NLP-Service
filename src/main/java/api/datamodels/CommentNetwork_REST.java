/**
 * Helper models for comment network
 * @author: Kevin
 */

package api.datamodels;

import java.util.ArrayList;

public class CommentNetwork_REST {
    private ArrayList<DeputyNode_REST> nodes = new ArrayList<>();
    private ArrayList<CommentLink_REST> links = new ArrayList<>();

    public ArrayList<DeputyNode_REST> getNodes() {return nodes;}
    public void setNodes(ArrayList<DeputyNode_REST> nodes) {this.nodes = nodes;}

    public ArrayList<CommentLink_REST> getLinks() {return links;}
    public void setLinks(ArrayList<CommentLink_REST> links) {this.links = links;}
}
