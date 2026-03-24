/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package za.ntier.report.fin;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MPeriod;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSequence;
import org.compiere.model.MYear;
import org.compiere.model.X_Fact_Acct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.report.MReportTree;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 *	Trial Balance Report
 *	
 *  @author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see https://sourceforge.net/p/adempiere/feature-requests/631/ 
 *  @version $Id: TrialBalance.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class TrialBalance_Detail extends SvrProcess
{
	/** AcctSchame Parameter			*/
	private int					p_C_AcctSchema_ID = 0;
	/**	Period Parameter				*/
	private int					p_C_Period_ID = 0;
	private Timestamp			p_DateAcct_From = null;
	private Timestamp			p_DateAcct_To = null;
	/**	Org Parameter					*/
	private int					p_AD_Org_ID = 0;
	/**	Account Parameter				*/
	private int					p_Account_ID = 0;
	private String				p_AccountValue_From = null;
	private String				p_AccountValue_To = null;
	/**	BPartner Parameter				*/
	private int					p_C_BPartner_ID = 0;
	/**	Product Parameter				*/
	private int					p_M_Product_ID = 0;
	/**	Project Parameter				*/
	private int					p_C_Project_ID = 0;
	/**	Activity Parameter				*/
	private int					p_C_Activity_ID = 0;
	/**	SalesRegion Parameter			*/
	private int					p_C_SalesRegion_ID = 0;
	/**	Campaign Parameter				*/
	private int					p_C_Campaign_ID = 0;
	/** Posting Type					*/
	//private String				p_PostingType = "A";
	/** Hierarchy						*/
	private int					p_PA_Hierarchy_ID = 0;

	private int					p_AD_OrgTrx_ID = 0;
	private int					p_C_LocFrom_ID = 0;
	private int					p_C_LocTo_ID = 0;
	private int					p_User1_ID = 0;
	private int					p_User2_ID = 0;
	private boolean				p_IsGroupByOrg = false;
	private StringBuffer		m_parameterWhere = new StringBuffer();
	private StringBuffer		m_parameterWhereBudget = new StringBuffer();
	private StringBuffer		m_parameterWhereActuals = new StringBuffer();
	private StringBuffer        m_YTD_Current = new StringBuffer();
	private StringBuffer        m_ZZ_YTD_Prior = new StringBuffer();
	private StringBuffer        m_ZZ_Prior_Year_Full = new StringBuffer();
	private StringBuffer        m_ZZ_Budget_YTD = new StringBuffer();
	private StringBuffer        m_ZZ_Total_Budget = new StringBuffer();
	private MSequence           sequence = null; 
	
	private static String		s_insert = "INSERT INTO T_TrialBalance_Detail_Ntier "
			+ "(AD_PInstance_ID, Fact_Acct_ID,"
			+ " AD_Client_ID, AD_Org_ID, Created,CreatedBy, Updated,UpdatedBy,"
			+ " C_AcctSchema_ID, Account_ID, AccountValue, DateTrx, DateAcct, C_Period_ID,"
			+ " AD_Table_ID, Record_ID, Line_ID,"
			+ " GL_Category_ID, GL_Budget_ID, C_Tax_ID, M_Locator_ID, PostingType,"
			+ " C_Currency_ID, AmtSourceDr, AmtSourceCr, AmtSourceBalance,"
			+ " AmtAcctDr, AmtAcctCr, AmtAcctBalance, C_UOM_ID, Qty,"
			+ " M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID, C_LocFrom_ID,C_LocTo_ID,"
			+ " C_SalesRegion_ID, C_Project_ID, C_Campaign_ID, C_Activity_ID,"
			+ " User1_ID, User2_ID, A_Asset_ID, Description, LevelNo, T_TrialBalance_Detail_Ntier_UU,"
			+ " ZZ_Account_Description,ZZ_YTD_Current,ZZ_YTD_Prior,ZZ_Prior_Year_Full,ZZ_Budget_YTD,ZZ_Total_Budget,"
			+ " ZZ_Variance_B_W,ZZ_Variance_YTD_Percent,ZZ_Annual_Budget_Remaining,T_TrialBalance_Detail_Ntier_ID,"
			+ " ZZ_Sur_Def_Group)";




	/**
	 *  Prepare - e.g., get Parameters.
	 */
	@Override
	protected void prepare()
	{
		StringBuilder sb = new StringBuilder ("AD_PInstance_ID=")
				.append(getAD_PInstance_ID());
		//	Parameter
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_Period_ID"))
				p_C_Period_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DateAcct"))
			{
				p_DateAcct_From = (Timestamp)para[i].getParameter();
				p_DateAcct_To = (Timestamp)para[i].getParameter_To();
			}
			else if (name.equals("PA_Hierarchy_ID"))
				p_PA_Hierarchy_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("Account_ID"))
				p_Account_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AccountValue"))
			{
				p_AccountValue_From = (String)para[i].getParameter();
				p_AccountValue_To = (String)para[i].getParameter_To();
			}
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("M_Product_ID"))
				p_M_Product_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_Project_ID"))
				p_C_Project_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_Activity_ID"))
				p_C_Activity_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_SalesRegion_ID"))
				p_C_SalesRegion_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_Campaign_ID"))
				p_C_Campaign_ID = ((BigDecimal)para[i].getParameter()).intValue();
			// else if (name.equals("PostingType"))
			// p_PostingType = (String)para[i].getParameter();
			else if (name.equals("IsGroupByOrg"))
				p_IsGroupByOrg = para[i].getParameterAsBoolean();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		//	Mandatory C_AcctSchema_ID
		m_parameterWhere.append("f.C_AcctSchema_ID=").append(p_C_AcctSchema_ID);
		//	Optional Account_ID
		if (p_Account_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID,MAcctSchemaElement.ELEMENTTYPE_Account, p_Account_ID));
		if (p_AccountValue_From != null && p_AccountValue_From.length() == 0)
			p_AccountValue_From = null;
		if (p_AccountValue_To != null && p_AccountValue_To.length() == 0)
			p_AccountValue_To = null;
		if (p_AccountValue_From != null && p_AccountValue_To != null)
			m_parameterWhere.append(" AND (f.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
			.append("WHERE f.Account_ID=ev.C_ElementValue_ID AND ev.Value >= ")
			.append(DB.TO_STRING(p_AccountValue_From)).append(" AND ev.Value <= ")
			.append(DB.TO_STRING(p_AccountValue_To)).append("))");
		else if (p_AccountValue_From != null && p_AccountValue_To == null)
			m_parameterWhere.append(" AND (f.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
			.append("WHERE f.Account_ID=ev.C_ElementValue_ID AND ev.Value >= ")
			.append(DB.TO_STRING(p_AccountValue_From)).append("))");
		else if (p_AccountValue_From == null && p_AccountValue_To != null)
			m_parameterWhere.append(" AND (f.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
			.append("WHERE f.Account_ID=ev.C_ElementValue_ID AND ev.Value <= ")
			.append(DB.TO_STRING(p_AccountValue_To)).append("))");
		//	Optional Org
		if (p_AD_Org_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_Organization, p_AD_Org_ID));
		//	Optional BPartner
		if (p_C_BPartner_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_BPartner, p_C_BPartner_ID));
		//	Optional Product
		if (p_M_Product_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_Product, p_M_Product_ID));
		//	Optional Project
		if (p_C_Project_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_Project, p_C_Project_ID));
		//	Optional Activity
		if (p_C_Activity_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_Activity, p_C_Activity_ID));
		//	Optional Campaign
		if (p_C_Campaign_ID != 0)
			m_parameterWhere.append(" AND f.C_Campaign_ID=").append(p_C_Campaign_ID);
		//	m_parameterWhere.append(" AND ").append(MReportTree.getWhereClause(getCtx(), 
		//		MAcctSchemaElement.ELEMENTTYPE_Campaign, p_C_Campaign_ID));
		//	Optional Sales Region
		if (p_C_SalesRegion_ID != 0)
			m_parameterWhere.append(" AND f.").append(MReportTree.getWhereClause(getCtx(), 
					p_PA_Hierarchy_ID, MAcctSchemaElement.ELEMENTTYPE_SalesRegion, p_C_SalesRegion_ID));
		m_parameterWhereBudget.append(m_parameterWhere.toString().replaceAll("f.", "fp."));
		m_parameterWhereActuals.append(m_parameterWhere.toString().replaceAll("f.", "fp."));
		//	Mandatory Posting Type
		//	m_parameterWhere.append(" AND PostingType='").append(p_PostingType).append("'");
		m_parameterWhereBudget.append(" AND fp.PostingType='").append(X_Fact_Acct.POSTINGTYPE_Budget).append("'");
		//
		//	setDateAcct();
		//	sb.append(" - DateAcct ").append(p_DateAcct_From).append("-").append(p_DateAcct_To);
		sb.append(" - Where=").append(m_parameterWhere);
		if (log.isLoggable(Level.FINE)) log.fine(sb.toString());
	}	//	prepare



	/**
	 *  Insert reporting data to T_TrialBalance
	 *  @return empty string
	 */
	protected String doIt()
	{
		sequence = MSequence.get(Env.getCtx(), "T_TrialBalance_Detail_Ntier", get_TrxName(), true);
		MPeriod mPeriodSelected = new MPeriod(getCtx(), p_C_Period_ID, get_TrxName());
		String SQL = "Select C_Period_ID from C_Period p where p.C_Year_ID = ? and p.periodNo = ?";
		int startID = DB.getSQLValue(get_TrxName(), SQL, mPeriodSelected.getC_Year_ID(),1);
		MPeriod startPeriod = new MPeriod(getCtx(), startID, get_TrxName());
		int lastID = DB.getSQLValue(get_TrxName(), SQL, mPeriodSelected.getC_Year_ID(),12);
		MPeriod lastPeriod = new MPeriod(getCtx(), lastID, get_TrxName());
		MYear currYear = new MYear(getCtx(), startPeriod.getC_Year_ID(), get_TrxName());
		int prevYear = Integer.parseInt(currYear.getFiscalYear());
		prevYear--;
		String SQL2 = "Select y.C_Year_ID from C_Year y where y.ad_client_id = ? and y.fiscalYear = ?";
		int prev_C_Year_ID = DB.getSQLValue(get_TrxName(), SQL2, getAD_Client_ID(),prevYear + "");
		int priorStartID = DB.getSQLValue(get_TrxName(), SQL, prev_C_Year_ID,1);
		MPeriod priorStartPeriod = new MPeriod(getCtx(), priorStartID, get_TrxName());
		int priorEndID = DB.getSQLValue(get_TrxName(), SQL, prev_C_Year_ID,mPeriodSelected.getPeriodNo());
		MPeriod priorEndPeriod = new MPeriod(getCtx(), priorEndID, get_TrxName());
		int priorLastID = DB.getSQLValue(get_TrxName(), SQL, prev_C_Year_ID,12);
		MPeriod priorLastPeriod = new MPeriod(getCtx(), priorLastID, get_TrxName());
		try {			
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='SDL'",null,null,false);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='SDL'","ev.ZZ_Det_Income_Group","'Subtotal - skills development levies'",false);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='OIN'",null,null,false);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='OIN'","ev.ZZ_Det_Income_Group","'Total other Income'",false);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"in ('OIN','SDL')","ev.AccountType","'Total Revenue'",false);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='ADM'",null,null,true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='ADM'","ev.ZZ_Det_Income_Group","'Subtotal - ADMINISTRATION EXPENSES'",true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='TQO'",null,null,true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='TQO'","ev.ZZ_Det_Income_Group","'Subtotal Transfer - QCTO'",true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='GRN'",null,null,true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"='GRN'","ev.ZZ_Det_Income_Group","'Subtotal Transfer - GRANTS EXPENDITURE'",true);
			createBalanceLine(startPeriod.getStartDate(), mPeriodSelected.getEndDate(), lastPeriod.getEndDate(),priorStartPeriod.getStartDate(),priorEndPeriod.getEndDate(),priorLastPeriod.getEndDate(),
					"in ('ADM','TQO','GRN')","ev.AccountType","'Total Expenses'",true);
			createSurplusDeficit_Catergory(" = 'ASD'","Administration Surplus/(Deficit)");
			createSurplusDeficit_Catergory(" = 'MGS'","Mandatory Grants Surplus/(Deficit)");
			createSurplusDeficit_Catergory(" = 'DGS'","Discretionary Grants Surplus/(Deficit)");
			createSurplusDeficit_Catergory(" in ('ASD','DGS','MGS')","Total Surplus/(Deficit)");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Database Error";
		}  

		return "";
	}	//	doIt


	private void setUpSumSQLs (Timestamp fromDate, Timestamp toDate, Timestamp lastDate,Timestamp priorStartDate,Timestamp priorEndDate, Timestamp priorLastDate,
			String zz_Det_Income_Group,String groupBy,boolean useParent) {
		/*
		m_YTD_Current.setLength(0);
		m_ZZ_YTD_Prior.setLength(0);
		m_ZZ_Prior_Year_Full.setLength(0);
		m_ZZ_Budget_YTD.setLength(0);
		m_ZZ_Total_Budget.setLength(0);

		m_YTD_Current =   m_YTD_Current.append("Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ")
				.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID")
				.append(" where ")
				.append(" fp.ad_client_id = f.ad_client_id");
		if (groupBy == null) {
			m_YTD_Current.append(" AND fp.Account_ID = f.Account_ID" );
		}
		m_YTD_Current.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(fromDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(toDate, true))
		.append(" + 1)")
		.append(" AND ").append(m_parameterWhereActuals)
		.append(" AND fp.PostingType='").append(p_PostingType).append("'");
		if (groupBy != null) {
			m_YTD_Current.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		 */

		/*
		m_ZZ_YTD_Prior = m_ZZ_YTD_Prior.append("Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ")
				.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID")
				.append(" where ")
				.append(" fp.ad_client_id = f.ad_client_id");
		if (groupBy == null) {
			m_ZZ_YTD_Prior.append(" AND fp.Account_ID = f.Account_ID" );
		}
		m_ZZ_YTD_Prior.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(priorStartDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(priorEndDate, true))
		.append(" + 1)")
		.append(" AND ").append(m_parameterWhereActuals)
		.append(" AND fp.PostingType='").append(p_PostingType).append("'");
		if (groupBy != null) {
			m_ZZ_YTD_Prior.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		 */

		/*
		m_ZZ_Prior_Year_Full = m_ZZ_Prior_Year_Full.append("Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ")
				.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID")
				.append(" where ")
				.append(" fp.ad_client_id = f.ad_client_id");
		if (groupBy == null) {
			m_ZZ_Prior_Year_Full.append(" AND fp.Account_ID = f.Account_ID" );
		}
		m_ZZ_Prior_Year_Full.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(priorStartDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(priorLastDate, true))
		.append(" + 1)")
		.append(" AND ").append(m_parameterWhereActuals)
		.append(" AND fp.PostingType='").append(p_PostingType).append("'");
		if (groupBy != null) {
			m_ZZ_Prior_Year_Full.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		 */

		/*
		m_ZZ_Budget_YTD = m_ZZ_Budget_YTD.append(" Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ")
				.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID")
				.append(" where ")
				.append(" fp.ad_client_id = f.ad_client_id");
		if (groupBy == null) {
			m_ZZ_Budget_YTD.append(" AND fp.Account_ID = f.Account_ID" );
		}
		m_ZZ_Budget_YTD.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(fromDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(toDate, true))
		.append(" + 1)")
		.append (" AND ").append(m_parameterWhereBudget);
		if (groupBy != null) {
			m_ZZ_Budget_YTD.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		 */
		/* 
		m_ZZ_Total_Budget = m_ZZ_Total_Budget.append(" Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ")
				.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID")
				.append(" where ")
				.append(" fp.ad_client_id = f.ad_client_id");
		if (groupBy == null) {
			m_ZZ_Total_Budget.append(" AND fp.Account_ID = f.Account_ID" );
		}
		m_ZZ_Total_Budget.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(fromDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(lastDate, true))
		.append(" + 1)")
		.append (" AND ").append(m_parameterWhereBudget);
		if (groupBy != null) {
			m_ZZ_Total_Budget.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		 */

		m_YTD_Current = setUpSumSQL(zz_Det_Income_Group,fromDate, toDate, groupBy,m_parameterWhereActuals,"A",useParent);
		m_ZZ_YTD_Prior = setUpSumSQL(zz_Det_Income_Group,priorStartDate, priorEndDate, groupBy,m_parameterWhereActuals,"A",useParent);
		m_ZZ_Prior_Year_Full = setUpSumSQL(zz_Det_Income_Group,priorStartDate, priorLastDate, groupBy,m_parameterWhereActuals,"A",useParent);
		m_ZZ_Budget_YTD = setUpSumSQL(zz_Det_Income_Group,fromDate, toDate, groupBy,m_parameterWhereBudget,"B",useParent);
		m_ZZ_Total_Budget = setUpSumSQL(zz_Det_Income_Group,fromDate, lastDate, groupBy,m_parameterWhereBudget,"B",useParent);
	}


	private StringBuffer setUpSumSQL(String zz_Det_Income_Group,Timestamp fromDate, Timestamp toDate, String groupBy,StringBuffer paraWhere,String postingType,boolean useParent) {
		StringBuffer sumSQL = new StringBuffer();
		sumSQL =   sumSQL.append("Select COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0) from Fact_Acct fp ");
		if (useParent) {
			sumSQL.append(" join ad_treenode tn1 on fp.account_ID = tn1.node_ID")
			.append(" join C_ElementValue ev1 on 	ev1.C_ElementValue_ID = tn1.parent_ID");
		} else {
			sumSQL.append(" join C_ElementValue ev1 on ev1.C_ElementValue_ID = fp.Account_ID");
		}
		sumSQL.append(" where ")
		.append(" fp.ad_client_id = f.ad_client_id");
		if (useParent) {
			sumSQL.append(" AND fp.Account_ID = ev1.C_ElementValue_ID" );
		} else {
			if (groupBy == null) {
				sumSQL.append(" AND fp.Account_ID = f.Account_ID" );
			}
		}
		sumSQL.append(" AND fp.DateAcct >= ").append(DB.TO_DATE(fromDate, true))
		.append(" AND fp.DateAcct < (").append(DB.TO_DATE(toDate, true))
		.append(" + 1)")
		.append(" AND ").append(paraWhere)
		.append(" AND fp.PostingType='").append(postingType).append("'");
		if (groupBy != null) {
			sumSQL.append(" AND ev1.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		return sumSQL;
	}


	private void createBalanceLine(Timestamp fromDate, Timestamp toDate, Timestamp lastDate,Timestamp priorStartDate,Timestamp priorEndDate, Timestamp priorLastDate,String zz_Det_Income_Group,
			String groupBy,String description,boolean useParent) throws Exception
	{
		setUpSumSQLs(fromDate,toDate,lastDate,priorStartDate,priorEndDate,priorLastDate,zz_Det_Income_Group,groupBy,useParent);
		StringBuilder sql = new StringBuilder (s_insert);
		//	(AD_PInstance_ID, Fact_Acct_ID,
		sql.append("SELECT ").append(getAD_PInstance_ID()).append(",0,");
		//	AD_Client_ID, AD_Org_ID, Created,CreatedBy, Updated,UpdatedBy,
		sql.append(getAD_Client_ID()).append(",");
		if (p_IsGroupByOrg)
			sql.append("AD_Org_ID");
		else if (p_AD_Org_ID == 0)
			sql.append("0");
		else
			sql.append(p_AD_Org_ID);
		sql.append(", getDate(),").append(getAD_User_ID())
		.append(",getDate(),").append(getAD_User_ID()).append(",");
		//	C_AcctSchema_ID, Account_ID, AccountValue, DateTrx, DateAcct, C_Period_ID,
		sql.append(p_C_AcctSchema_ID).append(",");
		if (groupBy == null && !useParent) {
			sql.append("Account_ID");
		} else {
			sql.append("null");
		}
		if (p_AccountValue_From != null)
			sql.append(",").append(DB.TO_STRING(p_AccountValue_From));
		else if (p_AccountValue_To != null)
			sql.append(",' '");
		else
			sql.append(",null");
		Timestamp balanceDay = p_DateAcct_From;
		sql.append(",null,").append(DB.TO_DATE(balanceDay, true)).append(",");
		if (p_C_Period_ID == 0)
			sql.append("null");
		else
			sql.append(p_C_Period_ID);
		sql.append(",");
		//	AD_Table_ID, Record_ID, Line_ID,
		sql.append("null,null,null,");
		//	GL_Category_ID, GL_Budget_ID, C_Tax_ID, M_Locator_ID, PostingType,
		sql.append("null,null,null,null,'").append("").append("',");
		//	C_Currency_ID, AmtSourceDr, AmtSourceCr, AmtSourceBalance,
		sql.append("null,null,null,null,");
		//	AmtAcctDr, AmtAcctCr, AmtAcctBalance, C_UOM_ID, Qty,
		sql.append(" COALESCE(SUM(AmtAcctDr),0),COALESCE(SUM(AmtAcctCr),0),"
				+ "COALESCE(SUM(AmtAcctDr),0)-COALESCE(SUM(AmtAcctCr),0),"
				+ " null,COALESCE(SUM(Qty),0),");
		//	M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID, C_LocFrom_ID,C_LocTo_ID,
		if (p_M_Product_ID == 0)
			sql.append ("null");
		else
			sql.append (p_M_Product_ID);
		sql.append(",");
		if (p_C_BPartner_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_BPartner_ID);
		sql.append(",");
		if (p_AD_OrgTrx_ID == 0)
			sql.append ("null");
		else
			sql.append (p_AD_OrgTrx_ID);
		sql.append(",");
		if (p_C_LocFrom_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_LocFrom_ID);
		sql.append(",");
		if (p_C_LocTo_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_LocTo_ID);
		sql.append(",");
		//	C_SalesRegion_ID, C_Project_ID, C_Campaign_ID, C_Activity_ID,
		if (p_C_SalesRegion_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_SalesRegion_ID);
		sql.append(",");
		if (p_C_Project_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_Project_ID);
		sql.append(",");
		if (p_C_Campaign_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_Campaign_ID);
		sql.append(",");
		if (p_C_Activity_ID == 0)
			sql.append ("null");
		else
			sql.append (p_C_Activity_ID);
		sql.append(",");
		//	User1_ID, User2_ID, A_Asset_ID, Description)
		if (p_User1_ID == 0)
			sql.append ("null");
		else
			sql.append (p_User1_ID);
		sql.append(",");
		if (p_User2_ID == 0)
			sql.append ("null");
		else
			sql.append (p_User2_ID);
		sql.append(", null, '");
		sql.append(Msg.getMsg(getCtx(), "opening.balance") + "', 0, generate_uuid() ");

		sql.append(",");
		if (description == null) {
			if (useParent) {
				sql.append("(Select e.Name from C_ElementValue e where e.C_ElementValue_ID = pev.C_ElementValue_ID)");
			} else {
				sql.append("(Select e.description from C_ElementValue e where e.C_ElementValue_ID = Account_ID)");
			}
		} else {
			sql.append(description);
		}

		// ZZ_YTD_Current
		sql.append(",");
		sql.append("(")
		.append(m_YTD_Current);
		sql.append(")");

		//ZZ_YTD_Prior
		sql.append(",");
		sql.append("(")
		.append(m_ZZ_YTD_Prior);
		sql.append(")");

		//ZZ_Prior_Year_Full
		sql.append(",");
		sql.append("(")
		.append(m_ZZ_Prior_Year_Full);
		sql.append(")");

		//ZZ_Budget_YTD
		sql.append(",");
		sql.append("(")
		.append(m_ZZ_Budget_YTD);
		sql.append(")");


		// ZZ_Total_Budget
		sql.append(",");
		sql.append("(")
		.append(m_ZZ_Total_Budget);
		sql.append(")");

		// ZZ_Variance_B_W   Actual YTD - Budget YTD
		sql.append(",");
		sql.append("(")
		.append("(").append(m_YTD_Current).append(")")
		.append("-")
		.append("(").append(m_ZZ_Budget_YTD).append(")");
		sql.append(")");

		// ZZ_Variance_YTD_Percent
		sql.append(",");
		sql.append("(")
		.append("(").append(m_YTD_Current).append(")")
		.append("-")
		.append("(").append(m_ZZ_Budget_YTD).append(")")
		.append("/")
		.append("nullif(")
		.append("(").append(m_ZZ_Budget_YTD).append(")")
		.append(",0)")
		.append(" * 100");
		sql.append(")");

		// ZZ_Annual_Budget_Remaining
		sql.append(",");
		sql.append("(")
		.append("(").append(m_ZZ_Total_Budget).append(")")
		.append("-")
		.append("(").append(m_YTD_Current).append(")");
		sql.append(")");

		// Key
		sql.append(",");
		sql.append("nextidfunc(" + sequence.getAD_Sequence_ID() + ",'N')");

		sql.append(",");
		if (useParent) {
			sql.append("pev.ZZ_Sur_Def_Group");
		} else {
			sql.append("ev.ZZ_Sur_Def_Group");
		}

		//
		sql.append(" FROM Fact_Acct f ")
		.append(" join C_ElementValue ev on ev.C_ElementValue_ID = f.Account_ID");
		if (useParent) {
			sql.append(" join ad_treenode tn on ev.C_ElementValue_ID = tn.node_ID")
			.append(" join C_ElementValue pev on pev.C_ElementValue_ID = tn.parent_ID");  // parent element value
		}
		sql.append(" WHERE f.AD_Client_ID=").append(getAD_Client_ID())
		.append (" AND ").append(m_parameterWhere)
		.append(" AND f.DateAcct >= ").append(DB.TO_DATE(priorStartDate, true))
		.append(" AND f.DateAcct < (").append(DB.TO_DATE(toDate, true))
		.append(" + 1)");
		if (useParent) {
			sql.append(" AND pev.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		} else {
			sql.append(" AND ev.ZZ_Det_Income_Group " + zz_Det_Income_Group );
		}
		//	Start Beginning of Year


		if (useParent) {
			if (groupBy != null) {
				sql.append(" GROUP BY f.Ad_Client_ID," + groupBy);
			} else {
				sql.append(" GROUP BY f.Ad_Client_ID,pev.C_ElementValue_ID");
				if (p_IsGroupByOrg)
					sql.append(", f.AD_Org_ID ");
			}
			sql.append(",")
			   .append("pev.ZZ_Sur_Def_Group");
		} else {		
			if (groupBy != null) {
				sql.append(" GROUP BY f.Ad_Client_ID," + groupBy);
			} else {
				sql.append(" GROUP BY f.Ad_Client_ID,f.Account_ID ");
				if (p_IsGroupByOrg)
					sql.append(", f.AD_Org_ID ");
			}	
			sql.append(",")
			   .append("ev.ZZ_Sur_Def_Group");
		}

		//
		int no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (no == 0)
			if (log.isLoggable(Level.FINE)) log.fine(sql.toString());
		if (log.isLoggable(Level.FINE)) log.fine("#" + no + " (Account_ID=" + p_Account_ID + ")");
	}	//	createBalanceLine




	private void createSurplusDeficit_Catergory(String zz_Sur_Def_Group_where,String description) throws Exception {
		StringBuilder sql = new StringBuilder (s_insert);
		//	(AD_PInstance_ID, Fact_Acct_ID,
		sql.append(" SELECT ").append(getAD_PInstance_ID());
		sql.append(",");
		sql.append("max(Fact_Acct_ID),")
		.append(" AD_Client_ID, AD_Org_ID, max(Created),max(CreatedBy), max(Updated),max(UpdatedBy),")
		.append(" max(C_AcctSchema_ID), max(account_ID), '', max(tr.DateTrx), max(tr.dateacct), max(C_Period_ID),")
		.append(" max(AD_Table_ID), max(Record_ID), max(Line_ID),")
		.append(" max(GL_Category_ID), max(GL_Budget_ID), max(C_Tax_ID), max(M_Locator_ID), max(PostingType),")
		.append(" null, sum(AmtSourceDr), sum(AmtSourceCr), sum(AmtSourceBalance),")
		.append(" sum(AmtAcctDr), sum(AmtAcctCr), sum(AmtAcctBalance), null, sum(Qty),")
		.append(" null, null, null, null,null,")
		.append(" null, null, null, null,")
		.append(" null, null, null, '', 0, generate_uuid(),")
		.append("'").append(description).append("'")
		.append(",")
		.append(" sum(ZZ_YTD_Current),sum(ZZ_YTD_Prior),sum(ZZ_Prior_Year_Full),sum(ZZ_Budget_YTD),sum(ZZ_Total_Budget),")
		.append(" sum(ZZ_Variance_B_W),0.00,sum(ZZ_Annual_Budget_Remaining),")
		.append("nextidfunc(" + sequence.getAD_Sequence_ID() + ",'N'),")
		.append("'XXX'");




		sql.append(" From T_TrialBalance_Detail_Ntier tr where tr.ZZ_Sur_Def_Group " + zz_Sur_Def_Group_where  );
		sql.append(" Group By ");
		sql.append(" AD_PInstance_ID");
		sql.append(",")
		.append(" AD_Client_ID, AD_Org_ID");
		
		int no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (no == 0)
			if (log.isLoggable(Level.FINE)) log.fine(sql.toString());
		if (log.isLoggable(Level.FINE)) log.fine("#" + no + " (Account_ID=" + p_Account_ID + ")");



	}


}	//	TrialBalance
