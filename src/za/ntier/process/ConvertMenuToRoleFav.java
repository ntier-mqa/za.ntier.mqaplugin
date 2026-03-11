package za.ntier.process;

import static org.compiere.model.SystemIDs.TREE_MENUPRIMARY;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Properties;

import org.adempiere.base.annotation.Parameter;
import org.compiere.model.MMenu;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTreeFavorite;
import org.compiere.model.MTreeFavoriteNode;
import org.compiere.model.MTreeNode;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

@org.adempiere.base.annotation.Process
public class ConvertMenuToRoleFav extends SvrProcess{
	@Parameter
	private String menus;
	
	@Parameter
	private String roles;
	
	@Override
	protected String doIt() throws Exception  {
		String [] menuIds = menus.split(",");
		String [] roleIds = roles.split(",");
		menus = "," + menus + ",";
		for (String roleId : roleIds) {
			int adRoleId = Integer.valueOf(roleId);
			int adTreeId = getTreeId(Env.getCtx(), adRoleId);
			
			// delete old one
			String queryTreeFavByRole = "SELECT AD_Tree_Favorite_ID FROM AD_Tree_Favorite WHERE IsActive='Y' AND AD_Role_ID=?";
			int treeFavID = DB.getSQLValue(null, queryTreeFavByRole, adRoleId);
			if (treeFavID > 0) {
				int oldFavNode = DB.executeUpdateEx("DELETE FROM AD_Tree_Favorite_Node WHERE AD_Tree_Favorite_ID = ?", 
						new Object [] {treeFavID}, 
						get_TrxName());
				addBufferLog(0, null, BigDecimal.valueOf(oldFavNode), "Number of old fav node", 0, 0);
			}else {
				MTreeFavorite treeFav = new MTreeFavorite(getCtx(), 0, get_TrxName());
				treeFav.setAD_Role_ID(adRoleId);
				treeFav.saveEx(get_TrxName());
				treeFavID = treeFav.getAD_Tree_Favorite_ID();
			}
			
			
			MTree mTree = new MTree(Env.getCtx(), adTreeId, false, true, null);
			
			addBufferLog(0, null, null, 
					String.format("Tree: %s - %s", 
							mTree.getName(),
							mTree.getAD_Tree_ID()
							),
					0, 0);
			
			MTreeNode rootNode = mTree.getRoot();
			
			findMenuNode(rootNode, treeFavID);
		}
		
		return null;
	}
	
	private void findMenuNode(MTreeNode parentNode, int treeFavID) {
		Enumeration<?> nodeEnum = parentNode.children();
		while(nodeEnum.hasMoreElements()){
			MTreeNode mChildNode = (MTreeNode)nodeEnum.nextElement();
			
			String sMenuIdOfNode = String.valueOf("," + mChildNode.getNode_ID()) + ",";
			if (menus.indexOf(sMenuIdOfNode) > -1) {
				// add this menu and all its child
				addFav(mChildNode, treeFavID, 0);
			}else if(mChildNode.isSummary()){
				// lookup on child
				findMenuNode(mChildNode, treeFavID);
			}else {
				// ignore this menu item by don't include on list
			}
        }
	}
	
	private void addFav (MTreeNode favNode, int treeFavID, int parentFavNode) {
		MTreeFavoriteNode createdFavNode = MTreeFavoriteNode.create(
				Env.getAD_Client_ID(Env.getCtx()), 
				Env.getContextAsInt(Env.getCtx(), Env.AD_ORG_ID), 
				treeFavID, 
				favNode.getNode_ID(), 
				parentFavNode,// parent 
				Integer.valueOf(favNode.getSeqNo()),// seq no
				favNode.getName(), 
				favNode.isSummary(), 
				true, 
				true,
				get_TrxName());
		MMenu menu = MMenu.get(favNode.getNode_ID());
		addBufferLog(0, null, null, 
				String.format("Fav Node: %s - %s Menu: %s", 
						createdFavNode.getName(), 
						favNode.isSummary(),
						menu.getName()
						),
				0, 0);
		
		if (favNode.isSummary()) {
			Enumeration<?> nodeEnum = favNode.children();
			while(nodeEnum.hasMoreElements()){
				MTreeNode mChildNode = (MTreeNode)nodeEnum.nextElement();
				addFav(mChildNode, treeFavID, createdFavNode.getAD_Tree_Favorite_Node_ID());
			}
		}
	}
	
	private int getTreeId(Properties ctx, int adRoleId)
    {
        int AD_Tree_ID = DB.getSQLValue(null,
                "SELECT COALESCE(r.AD_Tree_Menu_ID, ci.AD_Tree_Menu_ID)" 
                + "FROM AD_ClientInfo ci" 
                + " INNER JOIN AD_Role r ON (ci.AD_Client_ID=r.AD_Client_ID) "
                + "WHERE AD_Role_ID=?", adRoleId);
        if (AD_Tree_ID <= 0)
            AD_Tree_ID = TREE_MENUPRIMARY;    //  Menu
        return AD_Tree_ID;
    }

	@Override
	protected void prepare() {
		//do nothing
		
	}

}
