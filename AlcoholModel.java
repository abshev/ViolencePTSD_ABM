/*
 * Alcohol ABM
 *
 *     This model will compare interventions aimed at reducing racial disparities in alcohol-related
 * homicide, using New York City as the place and population of interest.
 * 
 *     The model class (1) creates the agents, neighborhoods, and physical space used by the model; 
 * (2) specifies the order of events occurring at each time step of the model; and (3) creates step reports 
 * and summary files of model results, as well as graphical displays of agent and neighborhood characteristics 
 * during the model run.
 * 
 * Revised May 26, 2015
 * 
 * Revisions:	June 2015 version adds alcohol interventions
 * 					-- intervention "2" randomly selects certain percentages of outlets to close
 * 					-- intervention "3" selects certain percentage of highest violence outlets to close
 * 					-- intervention "6" taxes alcohol
 */

package cbtModel;

import java.io.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.Color;

import org.joone.edit.jedit.InputHandler.insert_break;

import uchicago.src.sim.analysis.DataRecorder;
import uchicago.src.sim.analysis.Histogram;
import uchicago.src.sim.analysis.NumericDataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.OpenHistogram;
import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.engine.AbstractGUIController;
import uchicago.src.sim.engine.ActionGroup;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimpleModel;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.space.Object2DTorus;
import uchicago.src.sim.util.Random;
import uchicago.src.sim.util.SimUtilities;

import cern.jet.math.*;

public class AlcoholModel extends SimModelImpl {

		// variable declarations
		private Schedule			schedule;
		private Object2DGrid		agentSpace;
		private Object2DGrid		hoodSpace;
		private DisplaySurface 		displaySurf;
		private DataRecorder		recorder;
		private OpenSequenceGraph	agentTime;		
		private OpenSequenceGraph	agentNumber;
		private OpenSequenceGraph	hoodTime;
		private OpenSequenceGraph 	hoodChar;
		
	// MODEL INITIALIZATION PARAMETERS AND DEFAULT VALUES	
		
		// agent and world set-up		
		private int			numAgents=513000; 
		private int			worldXsize=400;
		private int			worldYsize=625;
		private int			numHoods=59;
		private int			numOutreach=1;			// number of outreach workers per neighborhood
			
		// duration of burn-in period and model run
		private int			startAging=10;
		private int			stopModelRun=500;
		
		// behavioral and influence parameters
		private int			lookForVictims=15;		// radius to look for victims of violence
		private double		alpha=0.10;				// neighborhood influence on drinking and violence
		private double		network_alpha=0.15;		// social network influence on drinking and violence
		private int			allowDeath=1; 			// 0 -- no mortality; 1 -- agent deaths allowed
		private int			agentRecycle=1;			// 0 -- no recycling; 1 -- deceased agent replaced with 18-year-old
		
		// output and displays
		private int			displayGUI=0;			// display grid of neighborhoods with agents pictured
		private int			outputAgentSteps=0;		// output agent step report, to check model as needed
		private int			outputHoodSteps=1;		// output neighborhood step report, to check model as needed

		
		// interventions
		private int			intervention=0;			// 0 -- no intervention, 1 -- increased social norms re: unacceptability of drunkenness
													// 2 -- decreased outlet density across all neighborhoods, 
													// 3 -- decreased number of most violent outlets in city
													// 4 -- increased community policing, 5 -- violence interrupters
													// 6 -- alcohol taxation, 7 -- close outlets early
													// 8 -- combined policing and violence interrupters
													// 9 -- decreased number of outlets in neighborhoods with highest outlet density
													// 10 -- close outlets early in neighborhoods with high outlet density
		private int			intTarget=0;			// 0 -- universal, 1 -- targeted
		private double 		intChange=0;			// amount by which intervention increases or decreases
		private int			intDuration=0;			// number of years to continue intervention
	
	// PARAMETERS TO BE INCLUDED IN CONTROL PANEL	
		public String[] getInitParam() {
			String[] initParams = { "NumAgents", "WorldXsize", "WorldYsize", "NumHoods", 
					"StartAging", "StopModelRun", "DisplayGUI", "LookForVictims", "LookForPolice", "Alpha", "Network_alpha", 
					"OutputAgentSteps", "OutputHoodSteps", "AllowDeath", "AgentRecycle",
					"Intervention", "IntTarget", "IntChange", "IntDuration",
					"LookForViolence", "LookForViolOutlets", "ReduceViol", "NumOutreach"};
			return initParams;
		}
		
	// CREATE STEP REPORT FILES
		


	// CREATE LISTS OF AGENTS, NEIGHBORHOODS, AND OUTLETS	
		
		// Lists of agents and neighborhoods
		public ArrayList<AlcoholAgent> agentList; 		// list of all agents
		public ArrayList<AlcoholAgent> tempagentList;	// temporary list of all agents, to use when shuffling
		public ArrayList<AlcoholAgent> wagentList;		// list of all white agents
		public ArrayList<AlcoholAgent> bagentList;		// list of all black agents
		public ArrayList<AlcoholAgent> hagentList;		// list of all hispanic agents
		public ArrayList<AlcoholAgent> oagentList;		// list of all other race agents
		public ArrayList<AlcoholAgent> magentList;		// list of male agents
		public ArrayList<AlcoholAgent> fagentList;		// list of female agents
		public ArrayList<AlcoholAgent> lesshsagentList;	// list of all agents with < high school education
		public ArrayList<AlcoholAgent> hsagentList;		// list of all agents with high school education or equivalent
		public ArrayList<AlcoholAgent> morehsagentList;	// list of all agents with more than a high school education
		public ArrayList<AlcoholAgent> baseNonDrkList;	// list of all agents who were non-drinkers at baseline
		public ArrayList<AlcoholAgent> baseLightDrkList;// list of all agents who were light/moderate drinkers at baseline
		public ArrayList<AlcoholAgent> baseHeavyDrkList;// list of all agents who were heavy drinkers at baseline
		public ArrayList<AlcoholNeighborhood> hoodList;	// list of all neighborhoods
		public ArrayList<AlcoholNeighborhood> temphoodList; // temporary list of all neighborhoods


		
		// Social network variables
		public static int numNodes;
		public static ArrayList<AlcoholAgent> SocialNetworkList;
	
	// VARIABLES FOR GRAPHS AND OUTPUT FILES	
		
		// variables storing data for graphs and output files
		private int numHeavyDrk;
		private double 	percHeavyDrk;
		private int numViolvict;
		private double 	percViolvict;
		private int	numPriorviolvict;
		private double	percPriorviolvict;
		private int numViolperp;
		private double	percViolperp;
		private int	numPriorviolperp;
		private double	percPriorviolperp;
		private int numDied;
		private double percDied;
		private int numMoved;
		private double percMoved;
		private double	avgHoodinc;
		private double	avgHoodviol;
		private double 	avgHoodstable;
		private double 	avgHoodheavy;
		private int	baselinePolice;
		
	// INITIALIZING MODEL	
		
		public static void main(String[] args) {
			SimInit init = new SimInit();
			AlcoholModel model = new AlcoholModel();
			init.loadModel(model, null, false);
		}
		
		public String getName() {
			return "AlcoholModel";
		}
		
		// Setup model
		public void setup() {
			System.out.println("Running setup");
			
			// Reset world where agents are located
			agentSpace = null;
			hoodSpace = null;
			
			// Only display grid with agent locations when not in multi-run (batch model) mode
			if (displayGUI == 1) {
				if (displaySurf != null) {
					displaySurf.dispose();
				}
				displaySurf = null;
				displaySurf = new DisplaySurface(this, "ViolenceCells");
				registerDisplaySurface("ViolenceCells", displaySurf);
			}
				
			// Reset list of agents
			agentList = new ArrayList<AlcoholAgent>();
			tempagentList = new ArrayList<AlcoholAgent>();
			// Reset race-specific lists of agents
			wagentList = new ArrayList<AlcoholAgent>();
			bagentList = new ArrayList<AlcoholAgent>();
			hagentList = new ArrayList<AlcoholAgent>();
			oagentList = new ArrayList<AlcoholAgent>();
			// Reset gender-specific lists of agents
			magentList = new ArrayList<AlcoholAgent>();
			fagentList = new ArrayList<AlcoholAgent>();
			// Reset education-specific lists of agents
			lesshsagentList = new ArrayList<AlcoholAgent>();
			hsagentList = new ArrayList<AlcoholAgent>();
			morehsagentList = new ArrayList<AlcoholAgent>();
			// Reset drinking-specific lists of agents
			baseNonDrkList = new ArrayList<AlcoholAgent>();
			baseLightDrkList = new ArrayList<AlcoholAgent>();
			baseHeavyDrkList = new ArrayList<AlcoholAgent>();
			// Reset social network list
			SocialNetworkList = new ArrayList<AlcoholAgent>();
			
			// Reset list of neighborhoods
			hoodList = new ArrayList<AlcoholNeighborhood>();
			temphoodList = new ArrayList<AlcoholNeighborhood>();
			

			
			
			// Reset schedule
			schedule = new Schedule(1);
			
		} // end of setup
		
		public void begin() {
			buildModel();
			buildSchedule();
		}
		
	/////////////////////////////////////// BUILD MODEL ///////////////////////////////////////	
		
		/*
		 * buildModel
		 * This part of the program creates the agent population, creates neighborhoods, and assigns agents to neighborhoods.  
		 * The buildModel function also creates displays to view model output in real-time as the model
		 * runs, and creates output files containing summary statistics as well as values of all variables at each time step.
		 * 
		 * 1 - Start random number generator
		 * 2 - Create physical space
		 * 3 - Create display surface to view physical space during the model run
		 * 4 - Create agents
		 * 5 - Create neighborhoods
		 * 6 - Assign agents to neighborhoods
		 * 7 - Calculate baseline neighborhood characteristics and assign to resident agents
		 * 8 - Assign preliminary substance use status
		 * 9 - Select initial alcohol outlet for drinking (and some non-drinking) agents
		 * 10 - Create social network linking agents to each other
		 * 11 - Create output files, step reports, and graphs of characteristics during the model run
		 * 
		 */
		public void buildModel() {
			System.out.println("Running BuildModel");
			System.out.println("Checking model: intervention = " + (int)getIntervention() + " and change = " + (int)(getIntChange()*100) + " and # steps = " + (int)getStopModelRun());
			
		// 1 - START RANDOM NUMBER GENERATOR
			buildModelStart();
			
		// 2 - CREATE PHYSICAL SPACE WHERE AGENTS RESIDE			
			agentSpace = new Object2DGrid(worldXsize, worldYsize);
			hoodSpace = new Object2DGrid(worldXsize, worldYsize);
			
		// 3 - CREATE DISPLAY SURFACE TO VIEW THE PHYSICAL SPACE DURING THE MODEL RUN
			if (displayGUI == 1) {
				displaySurf.addDisplayable(new Object2DDisplay(hoodSpace), "ViolenceCells");
				displaySurf.addDisplayable(new Object2DDisplay(agentSpace), "AgentWorld");
				displaySurf.display();
			}
			
		// 4 - CREATE AGENTS - including assignment of household income
			numNodes = numAgents;				// number of nodes for use in social network
			for (int i=0; i<numAgents; i++) {
				AlcoholAgent a = new AlcoholAgent();
				agentList.add(a);
				tempagentList.add(a);
			}
			System.out.printf("Created %d agents \n", agentList.size());
			
			// Create race-, gender-, and education-specific lists of agents
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				if (a.getRace()==1) {wagentList.add(a);}
					else if (a.getRace()==2) {bagentList.add(a);}
					else if (a.getRace()==3) {hagentList.add(a);}
					else if (a.getRace()==4) {oagentList.add(a);}
				if (a.getGender()==1) {magentList.add(a);}
					else if (a.getGender()==0) {fagentList.add(a);}
				if (a.getEducation()==1) {lesshsagentList.add(a);}
					else if (a.getEducation()==2) {hsagentList.add(a);}
					else if (a.getEducation()==3) {morehsagentList.add(a);}
			}
			
		// 5 - CREATE NEIGHBORHOODS
			for (int j=0; j<numHoods; j++) {
				AlcoholNeighborhood nb = new AlcoholNeighborhood(j, hoodSpace);
				hoodList.add(nb);
				temphoodList.add(nb);
				//System.out.printf("Created %d cells in neighborhood %d \n", nb.neighborhoodCellList.size(), nb.getID());
			}
			System.out.printf("Created %d neighborhoods \n", hoodList.size());
			
		// 6 - ASSIGN AGENTS TO NEIGHBORHOODS
			// Assign agents to neighborhoods so that neighborhoods match 59 NYC CDs as of 2000 in terms of
			// age, gender, race, household income, and population size
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				double randPopProb = Random.uniform.nextDoubleFromTo(0, 1);
				for (int j=0; j<hoodList.size(); j++) {
					if (randPopProb > a.popDist[j] && randPopProb <= a.popDist[j+1]) {
						a.setAgenthood(j);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(j);
						nb.neighborhoodAgentList.add(a);
						nb.temphoodAgentList.add(a);
						a.setAgenthood(nb.getID());
						a.setCdcode(nb.getCdcode());
						// Select X, Y location for agent within neighborhood boundaries
						int agentX= nb.getnb_minX() + (int)(Math.random() * (nb.getnb_maxX() - nb.getnb_minX()));
						int agentY= nb.getnb_minY() + (int)(Math.random() * (nb.getnb_maxY() - nb.getnb_minY()));
						a.setX(agentX);
						a.setY(agentY);
						agentSpace.putObjectAt(agentX, agentY, a);	
						// Notify cell that agent is present
						AlcoholCell newCell = (AlcoholCell)hoodSpace.getObjectAt(agentX, agentY);
						newCell.setMyAgent(a);
						newCell.setAgentIncome(a.getHouseincome());
						break;
					}
				}
			}
			
		// 7 - CALCULATE BASELINE NEIGHBORHOOD CHARACTERISTICS AND ASSIGN TO RESIDENT AGENTS
		//	   ALSO, CREATE PATROL AREAS FOR USE WITH TARGETED POLICING INTERVENTION	
			
			// Calculate average neighborhood characteristics at baseline
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood NB = (AlcoholNeighborhood)hoodList.get(t);
				setNBincome(NB);
				setNBviol(NB);
				setNBracecomp(NB);
				setNBstability(NB);
				setNByoungmale(NB);
				setNBheavydrk(NB);
				setNBmeanage(NB);
				
				// calculate neighborhood alcohol outlet density

			}
			
			// Identify neighborhoods with high levels of outlet density
			// Defined as top 25%
			Collections.sort(temphoodList, new Comparator() {
				public int compare(Object w1, Object w2) {
					AlcoholNeighborhood n1 = (AlcoholNeighborhood) w1;
					AlcoholNeighborhood n2 = (AlcoholNeighborhood) w2;
					// sort list in descending order, so that highest outlet density neighborhoods are first
					if (n1.getOutletdens() < n2.getOutletdens()) return 1;
					return 0;
				}
			});
			for (int w=0; w<15; w++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)temphoodList.get(w);
				nb.setHighdens(1);
			}
			
			// Identify neighborhoods with high levels of income and violence
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				if (nb.getAvghoodinc() > getAvgHoodinc()) {nb.setHighhoodinc(1);}
					else {nb.setHighhoodinc(0);}
				if (nb.getAvghoodviol() > getAvgHoodviol()) {nb.setHighhoodviol(1);}
					else {nb.setHighhoodviol(0);}
			}
			
			// Assign neighborhood characteristics to cells located in that neighborhood
			for (int i=0; i<worldXsize; i++) {
				for (int j=0; j<worldYsize; j++) {
					AlcoholCell newCell = (AlcoholCell)hoodSpace.getObjectAt(i,j);
					int cellID = newCell.getHoodID();
					AlcoholNeighborhood cellHood = (AlcoholNeighborhood)hoodList.get(cellID);
					newCell.setHighhoodinc(cellHood.getHighhoodinc());
					newCell.setHighhoodviol(cellHood.getHighhoodviol());
				}
			}
			
			// Identify whether agents live in high or low income neighborhoods at baseline
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.Agenthood);
				if (nb.getHighhoodinc()==1) { a.setEverHighInc(1); }
				else if (nb.getHighhoodinc()==0) { a.setEverLowInc(1); }
				if (a.getEverHighInc()==1) { a.setBaseIncHood(1); }
				else if (a.getEverLowInc()==1) { a.setBaseIncHood(2); }
			}
			


							
		// 8 - ASSIGN PRELIMINARY DRINKING STATUS 
			// Assign preliminary drinking status and preference for drinking in public place (i.e., for on-premises outlet)
			// as well as type of beverage preferred
			// Based on individual-level and neighborhood-level variables
			
			// baseline drinking status
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				
				// drinking status
				drinkingProb(a);
				
				// create lists of agents by drinking status at baseline
				if (a.getDrinkStat()==1) { baseNonDrkList.add(a); }
				else if (a.getDrinkStat()==2) { baseLightDrkList.add(a); }
				else if (a.getDrinkStat()==3) { baseHeavyDrkList.add(a); }
				
				
			}
			
		// 8b - CALCULATE ADDITIONAL NEIGHBORHOOD-LEVEL VARIABLES
			// Calculate average neighborhood characteristics related to drinking at baseline
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood NB = (AlcoholNeighborhood)hoodList.get(t);
				setNBlightdrk(NB);
				setNBheavydrk(NB);
			}
			

			
			

			
		// 10 - CREATE SOCIAL NETWORK
			createSocialNetwork();
			
			// Count number of friends who are abstainers, light/moderate drinkers, and heavy drinkers
			// and proportion of friends with negative attitudes towards drinking
			for (int i=0; i<agentList.size(); i++) {
				int numNoDrk = 0, numLightDrk = 0, numHeavyDrk = 0, numNegAtt = 0;
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				for (int w=0; w<a.getTotalFriends(); w++) {
					if (a.friendList.get(w).getDrinkStat()==1) { numNoDrk += 1; }
					else if (a.friendList.get(w).getDrinkStat()==2) { numLightDrk += 1; }
					else if (a.friendList.get(w).getDrinkStat()==3) { numHeavyDrk += 1; }
				}
				a.setNumFrdNoDrk(numNoDrk);
				a.setNumFrdLightDrk(numLightDrk);
				a.setNumFrdHeavyDrk(numHeavyDrk);
			}
			
		// 11 - CREATE OUTPUT FILES, STEP REPORTS, AND GRAPHS OF AGENT CHARACTERISTICS DURING THE MODEL RUN
			
			// Record output to file
			// NOTE: this function is included at the end of the file
			recordOutput();
			
			// Record neighborhood-specific output to file
			// NOTE: this function is only needed to check distributions of neighborhood characteristics
			// recordHoodOutput();
			
			// Graph agent characteristics during model run
			if (displayGUI == 1) {
				
				// Graph agent characteristics
				agentTime = new OpenSequenceGraph("Agent characteristics over time", this);
				agentTime.setXRange(0.0, 40.0);
				agentTime.setYRange(0.0, 100.0);
				agentTime.createSequence("% heavy drinker",  this,  "getPercHeavyDrk");
				agentTime.createSequence("% victimization", this, "getPercViolvict");
				agentTime.createSequence("% ever victimized", this, "getPercPriorviolvict");
				agentTime.createSequence("% perpetration", this, "getPercViolperp");
				agentTime.createSequence("% ever perpetrated", this, "getPercPriorviolperp");
				agentTime.createSequence("% died", this, "getPercDied");
				agentTime.createSequence("% moved", this, "getPercMoved");
				agentTime.display();
								
				// Graph neighborhood characteristics ranging from 0 to 1
				hoodTime = new OpenSequenceGraph("Neighborhood characteristics over time", this);
				hoodTime.setXRange(0.0, 40.0);
				hoodTime.setYRange(0.0, 1.0);
				hoodTime.createSequence("Avg hood violence", this, "getAvgHoodviol");
				hoodTime.createSequence("Percent heavy drinkers", this, "getAvgHoodheavy");
				hoodTime.createSequence("Percent 5 yr residents", this, "getAvgStable");
				hoodTime.display();
				
				/*
				// Histogram of number of light and heavy drinkers at outlets
				chartOutlet = new OpenHistogram("Outlets with light and heavy drinkers", 15, 0);
				chartOutlet.setXRange(0, 30);
				class outletLight implements BinDataSource {
					public double getBinValue(Object o) {
						AlcoholOutlet outlet = (AlcoholOutlet)o;
						return (double) outlet.getNLightDrk();
					}
				}
				class outletHeavy implements BinDataSource {
					public double getBinValue(Object o) {
						AlcoholOutlet outlet = (AlcoholOutlet)o;
						return (double) outlet.getNHeavyDrk();
					}
				}
				chartOutlet.createHistogramItem("# Light Drinkers", outletList, new outletLight(), 1, 0);
				chartOutlet.createHistogramItem("# Heavy Drinkers", outletList, new outletHeavy(), 1, 0);
				chartOutlet.display();	
				*/							
			}
			
			// Create agent step report to check model run, as needed
			if (outputAgentSteps == 1) {
				if (agentStepReportFile != null) 
					endStepReportFile();
				if (agentStepReportFileName.length() > 0) {
					agentStepReportFile = startAgentStepReportFile();
				}
				// header line for step report output file -- listing all variable names
				String header;
				header = String.format( "tick agentID agentX agentY agentHood age age2 age3 age4 age5 age6 ");
				header += String.format("gender race black hisp otherrace education hs morehs ");
				header += String.format("baseincome houseincome inc2 inc3 inc4 died pviolvict potviolvict violvict lastviolvict priorviolvict ");
				header += String.format("pviolperp potviolperp violperp lastviolperp priorviolperp ");
				header += String.format("probnondrk problightdrk probheavydrk ");
				header += String.format("lastdrinkstat drinkstat nondrk lightdrk heavydrk alcviol probhom homicide alchom ");
				header += String.format("probmove moved duration dur1 dur2 dur3 everhighinc everlowinc baseinchood ");
				header += String.format("assignfrd numfrd nodrkfrd moddrkfrd heavydrkfrd friendids closeearly");
				writeLineToStepReportFile ( header );
				stepReport();
			}
			
			// Create neighborhood step report to check model run, as needed
			if (outputHoodSteps == 1) {
				if (hoodStepReportFile != null) 
					endNBStepReportFile();
				if (hoodStepReportFileName.length() > 0) {
					hoodStepReportFile = startHoodStepReportFile();
				}
				// header line for step report output file -- listing all variable names
				String header;
				header = String.format( "tick hoodID avghoodinc lastavghoodinc changeinc ");
				header += String.format("hoodinc hoodinc1 hoodinc2 highhoodinc avghoodviol lastavghoodviol changeviol highhoodviol ");
				header += String.format("avghoodperp targethood pblack phisp pstable police ");
				header += String.format("plight pheavy avgage phom palchom nagent ncell");
				writeLineToNBStepReportFile ( header );				
				hoodStepReport();
			}
			
			

			
		} // end of buildModel
	
	//////////////////////////////////////////// STEPS OF THE MODEL ////////////////////////////////////////////	
		
		/*
		 * buildSchedule
		 * This part of the program implements the functions that occur at each step of the model.
		 * 
		 * Steps of the model
		 * 1 - Model stops when end condition is met
		 * 2 - Cell variables are reset
		 * 3 - Agents age one year
		 * 4 - Agent variables are reset
		 * 5 - Some agents die and are recycled
		 * 6 - Agents consider moving to a different neighborhood
		 * 7 - Update neighborhood characteristics after agent movement
		 * 8 - Drinking transitions and changes in preferred alcohol outlets (ALSO, ALCOHOL OUTLET INTERVENTION, when applicable)
		 * 9 - Update characteristics of alcohol outlets
		 * 10 - Reset locations of police officers (ALSO, POLICING INTERVENTION, when applicable)
		 * 11 - Identify potential victims and perpetrators of violence, including homicide
		 * 12 - Actual violent incidents take place
		 * 13 - Update neighborhood characteristics
		 * 14 - Grid of agent locations, real-time graphs, and output files are updated
		 */
		
		public void buildSchedule() {
			System.out.println("Running BuildSchedule");
			
			
			class ViolenceStep extends BasicAction {
				@SuppressWarnings("unchecked")
				public void execute() {
					
			// 1 - Stop the model after the specified number of time steps
					double currentTime = getTickCount();
					System.out.println("Running step " + (int)getTickCount());
					checkEndCondition();
					
			// 2 - Reset cell and neighborhood variables for current time step
					
					// Reset cell variables 
					for (int i=0; i < worldXsize; i++) {
						for (int j=0; j < worldYsize; j++) {
							AlcoholCell cell = (AlcoholCell)hoodSpace.getObjectAt(i, j);
							cell.resetCellVars();
						}
					}	
					

				
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.Agenthood);
						nb.setTargetHood(0);
						nb.setNumviolevent(0);
						nb.setNumOutreach(0);
						
			// 3 - Agents age one year (after burn-in period only)
						if (getTickCount()>startAging){ a.age+=1;}	
						
			// 4 - Reset agent variables for current time step
						
						// If agent died at last time step, reset relevant variables
						if (getTickCount()>startAging && a.getDied()==1) { resetDeath(a); }
						
						// Reset dummy variables and other indicators for all agents
						if (getTickCount()<=startAging || (getTickCount()>startAging && a.getDied()==0)) {
							a.resetVars(currentTime);
						}
						
			// 5 - Identify agents who will die at this time step
						
						if (allowDeath == 1) {	
							// Update mortality probabilities to account for changes in age category
							a.mortalityProb();
							
							// Identify agents who die at the current time step (after burn-in period)
							if (getTickCount()>startAging) {agentDeath(a);}
						}
						
			// 6 - Identify agents who move to a new neighborhood and find their new location
									
						if (getTickCount()>startAging) {
							
							// Recalculate moving probability based on duration of residence, income, and violence at last time step
							a.movingProb();
							
							// Identify agents who move
							double randomPmove = Random.uniform.nextDoubleFromTo(0,1);
							if (randomPmove < a.getPMove()) { a.setMoved(1); a.setDurationRes(0); }
								else {a.setMoved(0); a.durationRes += 1;}
						
							// Assign agents new location
							if (a.getMoved()==1) {
							
								// First, keep track of agent's old neighborhood but remove from agent list
								AlcoholNeighborhood oldhood = (AlcoholNeighborhood)hoodList.get(a.getAgenthood());
								oldhood.neighborhoodAgentList.remove(a);
								oldhood.temphoodAgentList.remove(a);
								
								// Second, update probabilities of living in each neighborhood based on current characteristics
								a.hoodProbDist3();
								a.hoodProbDist4();
							
								// Third, select new neighborhood
								double randPopProb = Random.uniform.nextDoubleFromTo(0, 1);
								for (int j=0; j<hoodList.size(); j++) {
									if (randPopProb > a.popDist[j] && randPopProb <= a.popDist[j+1] && j != a.getAgenthood()) {
										a.setAgenthood(j);
										AlcoholNeighborhood newhood = (AlcoholNeighborhood)hoodList.get(j);
										newhood.neighborhoodAgentList.add(a);
										newhood.temphoodAgentList.add(a);
										a.setAgenthood(newhood.getID());
										a.setCdcode(newhood.getCdcode());
										// Select X, Y location for agent within neighborhood boundaries
										int agentX= newhood.getnb_minX() + (int)(Math.random() * (newhood.getnb_maxX() - newhood.getnb_minX()));
										int agentY= newhood.getnb_minY() + (int)(Math.random() * (newhood.getnb_maxY() - newhood.getnb_minY()));
										a.setX(agentX);
										a.setY(agentY);
										agentSpace.putObjectAt(agentX, agentY, a);	
										// Notify cell that agent is present
										AlcoholCell newCell = (AlcoholCell)hoodSpace.getObjectAt(agentX, agentY);
										newCell.setMyAgent(a);
										newCell.setAgentIncome(a.getHouseincome());
										break;
									}
								}
							}
							
						}	
						
					} // end of agent loop	
					
			// 7 - Update neighborhood characteristics to reflect new residents after movement between neighborhoods
					
					for (int t=0; t<hoodList.size(); t++) {
						AlcoholNeighborhood NB = (AlcoholNeighborhood)hoodList.get(t);
						// setNBviol(NB);	// Note: not updating neighborhood violence because we want to keep consistent with previous time step
						// setNBperp(NB);	// not including violence history of people who moved into the area yet
						setNBstability(NB);
						setNByoungmale(NB);
						setNBlightdrk(NB);
						setNBheavydrk(NB);
						setNBracecomp(NB);
						setNBmeanage(NB);
						if (getTickCount()>startAging) {setNBincome(NB);}
					}
					
					// Identify neighborhoods with high levels of income
					for (int t=0; t<hoodList.size(); t++) {
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
						// if (nb.getAvghoodviol() > getAvgHoodviol()) {nb.setHighhoodviol(1);}
							// else {nb.setHighhoodviol(0);}
						if (nb.getAvghoodinc() > getAvgHoodinc()) {nb.setHighhoodinc(1);}
							else {nb.setHighhoodinc(0);}
					}
					
					// Update neighborhood characteristics of cells located in that neighborhood
					for (int i=0; i<worldXsize; i++) {
						for (int j=0; j<worldYsize; j++) {
							AlcoholCell newCell = (AlcoholCell)hoodSpace.getObjectAt(i,j);
							int cellID = newCell.getHoodID();
							AlcoholNeighborhood cellHood = (AlcoholNeighborhood)hoodList.get(cellID);
							newCell.setHighhoodviol(cellHood.getHighhoodviol());
						}
					}	
					
					// Identify whether agents live in high or low income neighborhoods
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.Agenthood);
						if (a.getMoved()==1) {
							if (nb.getHighhoodinc()==1) { a.setEverHighInc(1); }
							else if (nb.getHighhoodinc()==0) { a.setEverLowInc(1); }
						}
					}
					
					
			// 8 - Drinking transitions and changes in preferred alcohol outlets
			// ALCOHOL OUTLET DENSITY INTERVENTIONS ALSO OCCUR HERE WHEN IN EFFECT
			// REDUCED OUTLET HOURS INTERVENTIONS ALSO OCCUR HERE WHEN IN EFFECT
					

					
					/////////////////// EARLIER CLOSING TIMES
					// Close certain percentage of outlets in each neighborhood early (randomly selected or
					// in high-violence neighborhoods, or in neighborhoods with high outlet density)
					// NOTE THAT THE INTERVENTION OCCURS ONLY ONCE IN THE MODEL (at time step 11) 
					// BUT OUTLETS REMAIN CLOSED EARLY FOR THE DURATION OF THE MODEL RUN
					/////////////////////////////////////////////
								

					
					// EARLIER CLOSING TIMES INTERVENTION #3 -- HIGH OUTLET DENSITY NEIGHBORHOODS

						
							
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.getAgenthood());
						
						// Drinking transitions
						// NOTE: ALCOHOL TAXATION INTERVENTION AFFECTS DRINKING TRANSITIONS IN THIS STEP
						if (getTickCount()>startAging) { drinkingTrans(a); }
							
						// Potential selection of a new preferred alcohol outlet
						if (getTickCount()>startAging) {
							

							
							// make sure all non-drinkers have correct (non) beverage preference
							if (a.getDrinkStat() == 1) {
								a.setPreferBeer(0);
								a.setPreferWine(0);
								a.setPreferSpirit(0);
							}
							

							
							// non-drinker who remained a non-drinker?

							
							// current drinkers who made a transition in amount from last time step?
							// re-calculate preferred drinking location and select preferred outlet of that type

							
	
						}
					} // end of agent loop
					
					// Update number of friends who are abstainers, light/moderate drinkers, and heavy drinkers
					for (int i=0; i<agentList.size(); i++) {
						int numNoDrk = 0, numLightDrk = 0, numHeavyDrk = 0;
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						for (int w=0; w<a.getTotalFriends(); w++) {
							if (a.friendList.get(w).getDrinkStat()==1) { numNoDrk += 1; }
							else if (a.friendList.get(w).getDrinkStat()==2) { numLightDrk += 1; }
							else if (a.friendList.get(w).getDrinkStat()==3) { numHeavyDrk += 1; }
						}
						a.setNumFrdNoDrk(numNoDrk);
						a.setNumFrdLightDrk(numLightDrk);
						a.setNumFrdHeavyDrk(numHeavyDrk);
					}
					
	


					
			// 11 - Identify potential victims and perpetrators of violence, including homicide
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.Agenthood);	
								
						// Remember average level of violence and income in neighborhood at last time step
						nb.setLastavghoodviol(nb.getAvghoodviol());
						nb.setLastavghoodinc(nb.getAvghoodinc());
						
						// 12a - Calculate probabilities of homicide
						double logitPhom, ihomP1, ihomP2, ihomP3, ihomP4, logNhom, nhomP, homP;
						
						// Homicide 
						
						// INFLUENCE OF INDIVIDUAL LEVEL
						// 12.5.2014 -- calibration: increase intercept from -12.8404 to -10.5, back to -12.25
						//							 increase Inc1 coefficient from 2.1891 to 3.35, increase Age1 coefficient from 1.6775 to 2.5
						// logitPhom = (double) -12.25 + (3.35*a.getInc1()) + (1.3085*a.getInc2()) + (-0.0753*a.getInc3()) +
											// (1.8814*a.getGender()) + (2.5*a.getAge1()) + (1.3167*a.getAge2()) + (0.8021*a.getAge3()) + 
											// (0.6588*a.getAge4()) + (0.2296*a.getAge5());
						// calibration: decrease intercept from -15.35 to -16.75
						// 12.21.2015 -- calibration: trying equation above.
						// 12.28.2015 -- note: equation above is bad, switching back.
						// 12.28.2015 -- calibration: decrease intercept from -16.75 to -18.00
						// 12.30.2015 -- calibration: decrease intercept from -18.00 to -22.00
						// 1.4.2015 -- calibration: decrease intercept from -22 to -30
						// 1.5.2015 -- calibration: decrease intercept from -30 to -40
						// 1.6.2016 -- calibration: increase intercept from -40 to -35
						// 1.7.2016 -- calibration: increase intercept from -35 to -30
						// 1.11.2016 -- calibration: increase intercept from -30 to -15
						logitPhom = (double) -15.00 + (4.95*a.getInc1()) + (3.15*a.getInc2()) + (0.10*a.getInc3()) +
								(1.8814*a.getGender()) + (3.0*a.getAge1()) + (1.3167*a.getAge2()) + (0.8021*a.getAge3()) + 
								(0.6588*a.getAge4()) + (0.2296*a.getAge5());

						ihomP1 = Math.exp(logitPhom)/(1 + Math.exp(logitPhom));
						// increase probability of homicide if history of violence and/or heavy drinker
						// and decrease probability of homicide if no history of violence and/or not heavy drinker
						// 3.12.15 -- 50% increase for prior violence instead of 25%
						//			  100% increase for heavy drinker instead of 20%
						if (a.getPriorviolvict()==1 || a.getPriorviolperp()==1) { ihomP2 = ihomP1*1.50; } else { ihomP2 = ihomP1*0.75; }
						if (a.getHeavyDrinker()==1) { ihomP3 = ihomP2*2.0; } else { ihomP3 = ihomP2*0.80; }
						

						
						// INFLUENCE OF NEIGHBORHOOD LEVEL
						// neighborhood influences begin after burn-in period
						// 12.5.2014 -- calibration: increase intercept from -11.0397 to -10.0
						//							 increase PercBlack coefficient from 2.2516 to 3.15 to 3.50
						// 1.22.2015 -- calibration: add percent foreign-born and percent man/prof occupations
						if (getTickCount()>startAging) {
							// logNhom = (double) -10.4195 + (0.7292*nb.getHoodinc1()) + (0.6135*nb.getHoodinc2()) +
											   // (0.3409*nb.getAvghoodviol()) + (-0.1699*nb.getPercLightDrk()) + 
											   // (-0.182*nb.getPercHeavyDrk()) + (2.045*nb.getPercBlack()) + 
											   // (1.3021*nb.getPercHisp()) + (-0.0125*nb.getPercFBorn()) + 
											   // (-0.0185*nb.getPercManProf());
							// 3.12.15 -- calibration: increase hoodinc1 coefficient from 1.80 to 2.20
							//			  increase hoodinc2 coefficient from 0.70 to 0.85
							//			  increase avghoodviol coefficient from 0.90 to 1.25
							// logNhom = (double) -9.75 + (2.20*nb.getHoodinc1()) + (0.85*nb.getHoodinc2()) +
											 //  (1.25*nb.getAvghoodviol()) + (-0.10*nb.getPercLightDrk()) + 
											 //  (-0.12*nb.getPercHeavyDrk()) + (5.15*nb.getPercBlack()) + 
											 //  (3.0*nb.getPercHisp()) + (-0.025*nb.getPercFBorn()) + 
											 //  (-0.03*nb.getPercManProf());
							// 3.12.15 -- calibration: increase intercept from -12.461 to -11.0
							//			  increase hoodinc1 coefficient from 0.3880 to 2.50
							//			  increase hoodinc2 coefficient from 0.5214 to 0.85
							//			  increase avghoodviol coefficient from 0.0294 to 1.25
							//			  increase percblack coefficient from 1.3834 to 5.75
							// 			  increase perchisp coefficient from 0.1293 to 2.75
							//			  decrease percmanprof coefficient from 0.0101 to -0.005
							//			  decrease percstable coefficient from 1.2459 to 0.01
							//			  increase percunemp from 1.4887 to 2.20
							// 3.22.15 -- calibration: decrease intercept form -11.0 to -11.15
							//			  decrease percblack coefficient from 5.75 to 5.30
							//			  decrease percyoungmale coefficient from 8.4289 to 8.10
							// 1.5.16 --  decrease percblack coefficient from 5.30 to 3.00
							logNhom = (double) -11.15 + (2.50*nb.getHoodinc1()) + (0.85*nb.getHoodinc2()) +
											   (1.25*nb.getAvghoodviol()) + (-0.0834*nb.getPercLightDrk()) +
											   (-0.044*nb.getPercHeavyDrk()) + (3.00*nb.getPercBlack()) +
											   (2.75*nb.getPercHisp()) + (-0.007*nb.getPercFBorn()) +
											   (-0.005*nb.getPercManProf()) + (8.10*nb.getPercYoungMale()) +
											   (0.01*nb.getPercStable()) + (2.20*nb.getPercUnemp()) +
											   (3.9179*nb.getPercFemHHKids());
							nhomP = Math.exp(logNhom);
						} else nhomP = ihomP3;
						
						// FINAL PROBABILITIY
						if (getTickCount()>startAging) { homP = ((1 - getNetwork_alpha() - getAlpha())*(ihomP3)) + (getAlpha()*nhomP); }
						else { homP = ((1 - getNetwork_alpha())*ihomP3); }
						
						a.setProbHomicide(homP);
						
						// 12b - Calculate probabilities of violent victimization
						double logitP1, logitP2;
						double iviolP1, iviolP2, iviolP3, iviolP4;		// individual-level probabilities
						double logitN1, logitN2;
						double nviolP1, nviolP2;						// neighborhood-level probabilities
						double violP1, violP2;
						
						// Non-fatal violent victimization
						
						// INFLUENCE OF INDIVIDUAL LEVEL
						// 11.20.2014 -- revised equation to change reference groups
						// calibration: decrease intercept from -3.8974 to -5.50
						//				increase Lesshs coefficient from 0.8145 to 1.45
						//				increase HS coefficient from -0.7195 to 0.90
						//				increase Inc1 coefficient from 0.2534 to 1.75
						//				increase Inc2 coefficient from 0.20 to 0.55
						//				increase Age1 coefficient from 1.7068 to 2.2
						// calibration: decrease intercept from -5.80 to -6.75
						// 12.21.15 -- increase intercept from -6.75 to -5.80
						// 12.30.15 -- increase intercept from -5.80 to -5.00
						// 1.4.16 -- decrease intercept from -5.00 to -5.50
						// 1.5.16 -- decrease intercept from -5.50 to -5.75
						// 1.6.16 -- increase intercept from -5.75 to -5.65
						// 1.7.16 -- decrease intercept from -5.65 to -5.70
						logitP1 = (double) -5.70 + (0.2796*a.getGender()) + (2.2*a.getAge1()) +
								(0.85*a.getAge2()) + (0.5763*a.getAge3()) + (0.0143*a.getAge4()) +
								(-0.17*a.getAge5()) + (1.45*a.getLesshs()) + (0.90*a.getHs()) +
								(1.75*a.getInc1()) + (0.55*a.getInc2()) + (0.128*a.getInc3()) +
								(-0.6113*a.getLightDrinker()) + (0.6341*a.getHeavyDrinker()) +
								(1.614*a.getPriorviolvict()) + (0.4095*a.getPriorviolperp());
						iviolP1 = Math.exp(logitP1)/(1 + Math.exp(logitP1));
						

						
						// INFLUENCE OF NEIGHBORHOOD LEVEL
						// neighborhood influences begin after burn-in period
						// 11.19.2014 -- calibration: increase intercept from -3.6763 to -3.00
						// 12.4.2014 -- calibration: increase percblack coefficient from -0.5331 to 2.50
						//							 increase perchisp coefficient from -1.5628 to 0.30
						if (getTickCount()>startAging) {
							// logitN1 = (double) -3.00 + (0.3995*nb.getHoodinc1()) + (0.0248*nb.getHoodinc2()) +
									//   (2.50*nb.getPercBlack()) + (0.30*nb.getPercHisp()) + 
									//   (14.7472*nb.getAvghoodviol());
							// 3.12.2015 -- calibration: decrease intercept from 1.7728 to 0.25 to -2.20
							//			    increase hoodinc1 coefficient from 0.6044 to 3.5
							//				increase hoodinc2 coefficient from 0.0777 to 1.5
							//				increase percblack coefficient from 0.0053 to 6.75
							//				increase perchisp coefficient from -1.3231 to 2.50
							//				decrease percyoungmale coefficient from 23.1486 to 10.0
							//			    increase percstable coefficient from -7.6444 to -0.50
							//				increase unemp coefficient from -9.0505 to 5.0
							//				increase femhhkids coefficient from 0.3383 to 4.50
							// 12.30.2015   increase percblack coefficient from 6.75 to 7.25
							// 1.5.2016   increase percblack coefficient from 7.25 to 8.50
							// 1.6.2016     increase percblack coefficient from 8.50 to 10.00
							// 1.7.2016     increase percblack coefficient from 10.00 to 12.00
							// 1.11.2016    increase percblack coefficient from 12 to 20
							logitN1 = (double) -2.20 + (3.5*nb.getHoodinc1()) + (1.5*nb.getHoodinc2()) +
									  (20.00*nb.getPercBlack()) + (2.5*nb.getPercHisp()) +
									  (16.4594*nb.getAvghoodviol()) + (10.0*nb.getPercYoungMale()) +
									  (-0.50*nb.getPercStable()) + (5.00*nb.getPercUnemp()) +
									  (4.50*nb.getPercFemHHKids());
							nviolP1 = Math.exp(logitN1)/(1 + Math.exp(logitN1));
						} else nviolP1 = 0;
						
						// FINAL PROBABILITY
						if (getTickCount()>startAging) { violP1 = ((1 - getAlpha() - getNetwork_alpha())*(iviolP1)) + (getAlpha()*nviolP1); }
						else { violP1 = ((1 - getNetwork_alpha())*iviolP1); }
						
						a.setPviolvict(violP1);
						
						// 12c - Calculate probability of violent perpetration
						
						// INFLUENCE OF INDIVIDUAL LEVEL
						// 11.20.2014 -- revised equation to change reference categories
						// calibration: decrease intercept from -6.2805 to -6.75 to -7.50
						//				increase coefficient for Age2 from 0.0985 to 1.25
						//				increase coefficient for Age3 from -0.3876 to 0.15
						//				increase coefficient for Lesshs from 0.3332 to 0.90 to 1.00
						//				increase coefficient for HS from 0.2902 to 0.65
						// 				increase coefficient for Inc1 from -0.1394 to 0.75 to 0.95
						//				increase coefficient for Inc2 from -0.3918 to 0.40 to 0.55
						// 				increase coefficient for Inc3 from -0.3904 to 0.125
						//				increase coefficient for priorviolperp from 1.0548 to 1.25
						// 12.21.2015   increase intercept from -8.50 to -7.50
						// 1.5.2016     decrease intercept from -7.50 to -7.60
						// 1.6.2016     decrease intercept from -7.60 to -7.70
						// 1.7.2016     decrease intercept from -7.70 to -7.80
						// 1.11.2016    decrease intercept from -7.80 to 8.00
						//              
						logitP2 = (double) -8.00 + (1.0901*a.getGender()) + (1.1434*a.getAge1()) +
								(1.25*a.getAge2()) + (0.15*a.getAge3()) + (-0.9339*a.getAge4()) +
								(-2.3138*a.getAge5()) + (1.00*a.getLesshs()) + (0.65*a.getHs()) +
								(0.95*a.getInc1()) + (0.55*a.getInc2()) + (0.125*a.getInc3()) +
								(0.0072*a.getLightDrinker()) + (0.4521*a.getHeavyDrinker()) +
								(2.1887*a.getPriorviolvict()) + (1.25*a.getPriorviolperp());
						iviolP3 = Math.exp(logitP2)/(1 + Math.exp(logitP2));
						

						
						// INFLUENCE OF NEIGHBORHOOD LEVEL
						// neighborhood influences begin after burn-in period
						// 11.19.2014 -- calibration: increase intercept from -4.9017 to -3.75
						// 12.4.2014 -- calibration: increase percblack coefficient from -0.5331 to 2.50
						//							 increase perchisp coefficient from -1.5628 to 0.30
						// 12.28.2015 -- calibration: increase percblack coefficient from 6.75 to 7.00
						// 1.4.2016 -- calibration: increase percblack coefficient from 7.00 to 8.00
						// 1.5.2016 -- calibration: increase percblack coefficient from 8.00 to 10.00
						// 1.6.2016 -- calibration: increase percblack coefficient from 10.00 to 12.00
						// 1.11.2016 -- calibration: increase percblack coefficinet from 12 to 20.
			
						if (getTickCount()>startAging) {
							// logitN2 = (double) -3.75 + (0.3995*nb.getHoodinc1()) + (0.0248*nb.getHoodinc2()) +
									//  (2.50*nb.getPercBlack()) + (0.30*nb.getPercHisp()) + 
									//  (14.7472*nb.getAvghoodviol());
							logitN2 = (double) -4.40 + (3.5*nb.getHoodinc1()) + (1.5*nb.getHoodinc2()) +
									  (20.00*nb.getPercBlack()) + (2.5*nb.getPercHisp()) +
									  (16.4594*nb.getAvghoodviol()) + (10.0*nb.getPercYoungMale()) +
									  (-0.50*nb.getPercStable()) + (5.00*nb.getPercUnemp()) +
									  (4.50*nb.getPercFemHHKids());
							nviolP2 = Math.exp(logitN2)/(1 + Math.exp(logitN2));
						} else nviolP2 = 0;
						
						// FINAL PROBABILITY
						if (getTickCount()>startAging) { violP2 = ((1 - getAlpha() - getNetwork_alpha())*(iviolP3)) + (getAlpha()*nviolP2); }
						else { violP2 = ((1 - getNetwork_alpha())*iviolP3); }
						
						a.setPviolperp(violP2);
						
						// Variable containing highest probability of violence
						double max1 = Math.max(a.getProbHomicide(), a.getPviolvict());
						double max2 = Math.max(a.getPviolvict(), a.getPviolperp());
						double max3 = Math.max(max1, max2);
						a.setProbViolence(max3);
						
					} // close the agent loop
					

					////////////// IF VIOLENCE INTERRUPTER INTERVENTION IS IN EFFECT
					// Sort all agents in designated neighborhood by probabilities of violence
					// Simulate matching violence interrupter to 10 agents with highest probability of violence
					// Discount probabilities of violence by half for these agents

					
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i); 
					
						// 12d - Potential victim of homicide
						double randomPhom = Random.uniform.nextDoubleFromTo(0,1);
						if (randomPhom < a.getProbHomicide()) {a.setPothomicide(1);}
							else {a.setPothomicide(0);}
						
						// 12e - Potential victim of violence
						double randomP1 = Random.uniform.nextDoubleFromTo(0,1);
						if (randomP1 < a.getPviolvict()) {a.setPotviolvict(1);}
							else {a.setPotviolvict(0);}
						
						// 12f - Potential perpetrator of violence
						double randomP2 = Random.uniform.nextDoubleFromTo(0,1);
						if (randomP2 < a.getPviolperp()) {a.setPotviolperp(1);}
							else {a.setPotviolperp(0);}
					
						// 12g - Notify agent's cell that there is a potential victim present
						AlcoholCell victimCell = (AlcoholCell)hoodSpace.getObjectAt(a.getX(), a.getY());
						if (a.getPotviolvict() == 1 || a.getPothomicide() == 1) { victimCell.setPotVictim(1);}
						
					}	
							
			// 13 - Actual violent incidents
					
					// Cycle through potential perpetrators
					// to see if there are any potential victims of violence
					// within the specified distance
					// if there are, they become true victims and a proportion of the nearby agents witness the assault
					// NOTE: first shuffle agent list so same agents aren't always perpetrating first
					SimUtilities.shuffle(tempagentList);				
					for (int i=0; i<tempagentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)tempagentList.get(i);
						if (a.getPotviolperp()==1) {findVictims(a);}
					}
					
					// 13b - Update violent characteristics of alcohol outlets

					
					// 13c - Update number of friends who were victimized or who perpetrated violence
					for (int i=0; i<agentList.size(); i++) {
						int numVictim = 0, numPerp = 0;
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						for (int w=0; w<a.getTotalFriends(); w++) {
							if (a.friendList.get(w).getViolvict()==1) { numVictim += 1; }
							if (a.friendList.get(w).getViolperp()==1) { numPerp += 1; }
						}
						a.setNumFrdVictim(numVictim);
						a.setNumFrdPerp(numPerp);
					}
					
			// 14 - Update average neighborhood characteristics
					for (int t=0; t<hoodList.size(); t++) {
						AlcoholNeighborhood NB = (AlcoholNeighborhood)hoodList.get(t);
						setNBviol(NB);
						setNBperp(NB);
						setNBlightdrk(NB);
						setNBheavydrk(NB);
						setNBmeanage(NB);
						setNByoungmale(NB);
						if (getTickCount()>startAging) {setNBincome(NB);}
					}
					
					// Identify neighborhoods with high levels of violence and income
					for (int t=0; t<hoodList.size(); t++) {
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
						if (nb.getAvghoodviol() > getAvgHoodviol()) {nb.setHighhoodviol(1);}
							else {nb.setHighhoodviol(0);}
						if (nb.getAvghoodinc() > getAvgHoodinc()) {nb.setHighhoodinc(1);}
							else {nb.setHighhoodinc(0);}
					}
					
					// Update neighborhood characteristics of cells located in that neighborhood
					for (int i=0; i<worldXsize; i++) {
						for (int j=0; j<worldYsize; j++) {
							AlcoholCell newCell = (AlcoholCell)hoodSpace.getObjectAt(i,j);
							int cellID = newCell.getHoodID();
							AlcoholNeighborhood cellHood = (AlcoholNeighborhood)hoodList.get(cellID);
							newCell.setHighhoodviol(cellHood.getHighhoodviol());
						}
					}
					

					
			// 15 - Update grid of agent locations, real-time graphs, and output file
					
					if (displayGUI == 1) {
						// Update display
						displaySurf.updateDisplay();
						// Update graphs
						agentTime.record();
						agentTime.updateGraph();
						hoodTime.record();
						hoodTime.updateGraph();
						//chartOutlet.step();
					}
				
					// Record output to file
					recorder.record();
					recorder.writeToFile();
					
					// Output agent characteristics at each time step, as needed
					if (outputAgentSteps == 1) stepReport();
					
					// Output neighborhood characteristics at each time step, as needed
					if (outputHoodSteps == 1) hoodStepReport();
					

					
					
				}
			}
			
			schedule.scheduleActionBeginning(0, new ViolenceStep());
			
		} // end of buildSchedule
		
		
		/////////////////////////////// FUNCTIONS CALLED ABOVE
		
		// buildModelStart()
		// Set random number generators
		public void buildModelStart() {
			uchicago.src.sim.util.Random.createUniform();
			uchicago.src.sim.util.Random.createNormal( 0.0, 1.0 );
			uchicago.src.sim.util.Random.createBeta(2,5);
		}
		
		// checkEndCondition()
		// Stop model run when end time is reached
		public void checkEndCondition() {
			if (getTickCount()>=stopModelRun) { System.out.println("End of model run"); this.stop();}
		}
		
		// setNBincome()
		// Calculate average neighborhood income among agent residents at baseline
		public void setNBincome( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalAgentIncome = 0;
			double avgAgentIncome = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				// assign midpoint values of agent income categories
				// 1 < $10k, 2 $10-$14, 3 $15-$19, 4 $20-$24, 5 $25-$29, 6 $30-$34, 7 $35-$39, 8 $40-$44, 9 $45-$49
				// 10 $50-$59, 11 $60-$74, 12 $75-$99, 13 $100-$124, 14 $125-$149, 15 $150-$199, 16 $200+
				int AgentInc = 0;
				if (a.getHouseinc16() == 1) {AgentInc = 5000;}
					else if (a.getHouseinc16() == 2) {AgentInc = 12500;}
					else if (a.getHouseinc16() == 3) {AgentInc = 17500;}
					else if (a.getHouseinc16() == 4) {AgentInc = 22500;}
					else if (a.getHouseinc16() == 5) {AgentInc = 27500;}
					else if (a.getHouseinc16() == 6) {AgentInc = 32500;}
					else if (a.getHouseinc16() == 7) {AgentInc = 37500;}
					else if (a.getHouseinc16() == 8) {AgentInc = 42500;}
					else if (a.getHouseinc16() == 9) {AgentInc = 47500;}
					else if (a.getHouseinc16() == 10) {AgentInc = 55000;}
					else if (a.getHouseinc16() == 11) {AgentInc = 67500;}
					else if (a.getHouseinc16() == 12) {AgentInc = 87500;}
					else if (a.getHouseinc16() == 13) {AgentInc = 112500;}
					else if (a.getHouseinc16() == 14) {AgentInc = 137500;}
					else if (a.getHouseinc16() == 15) {AgentInc = 175000;}
					else {AgentInc = 225000;}
				totalAgentIncome += AgentInc;
			}
			// calculate average income of agents in neighborhood
			if (totalAgentNum>0) {
				avgAgentIncome = (double) totalAgentIncome/totalAgentNum;
			}
			nb.setAvghoodinc(avgAgentIncome);
			// neighborhood income categories
			if (avgAgentIncome < 44000) {nb.setHoodinc(1);}
				else if (avgAgentIncome >= 44000 & avgAgentIncome < 58000) {nb.setHoodinc(2);}
				else if (avgAgentIncome >= 58000) {nb.setHoodinc(3);}
			// dummy variables for neighborhood income categories
			if (nb.getHoodinc()==1) {nb.setHoodinc1(1);}
				else nb.setHoodinc1(0);
			if (nb.getHoodinc()==2) {nb.setHoodinc2(1);}
				else nb.setHoodinc2(0);
		} // end of setNBincome()
		
		// setNBviol()
		// Calculate average level of violence and homicide rate among agent residents
		public void setNBviol( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalAgentViol = 0;
			int totalHom = 0;
			int totalAlcHom = 0;
			double avgAgentViol = -1;
			double avgHom = -1;
			double avgAlcHom = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				totalAgentViol += a.getViolvict();
				totalHom += a.getHomicide();
				totalAlcHom += a.getAlcHomicide();
			}
			// calculate rates of violence and homicide among agents in neighborhood, expressed as proportion
			if (totalAgentNum>0) {
				avgAgentViol = (double) totalAgentViol/totalAgentNum;
				avgHom = (double) totalHom/totalAgentNum;
				avgAlcHom = (double) totalAlcHom/totalAgentNum;
			}
			nb.setAvghoodviol(avgAgentViol);
			nb.setHomrate(avgHom);
			nb.setAlchomrate(avgAlcHom);
		} // end of setNBviol()
		
		// setNBracecomp()
		// Calculate percent Black and Hispanic neighborhood residents
		public void setNBracecomp( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalBlackAgent = 0;
			int totalHispAgent = 0;
			double avgPBlack = -1;
			double avgPHisp = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				totalBlackAgent += a.getBlack();
				totalHispAgent += a.getHisp();
			}
			// calculate percent black and hispanic agents in neighborhood, expressed as proportion
			if (totalAgentNum>0) {
				avgPBlack = (double) totalBlackAgent/totalAgentNum;
				avgPHisp = (double) totalHispAgent/totalAgentNum;
			}
			nb.setPercBlack(avgPBlack);
			nb.setPercHisp(avgPHisp);
		} // end of setNBracecomp()
		
		// setNBmeanage()
		// Calculate average age of neighborhood residents
		public void setNBmeanage( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalAge = 0;
			double avgAge = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = (AlcoholAgent)nb.getNeighborhoodAgentList().get(n);
				totalAge += a.getAge();
			}
			if (totalAgentNum > 0) {
				avgAge = (double) totalAge/totalAgentNum;
			}
			nb.setAvgAge(avgAge);
		} // end of setNBmeanage
		
		// setNByoungmale()
		// Calculate percent young males aged 18-24 yrs old
		public void setNByoungmale( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalYMale = 0;
			double avgPYMale = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				if (a.getGender() == 1 && a.getAgecat() == 1) { totalYMale += 1; }
			}
			// calculate percent young males aged 18-24 yrs old
			if (totalAgentNum>0) {
				avgPYMale = (double) totalYMale/totalAgentNum;
			}
			nb.setPercYoungMale(avgPYMale);
		} // end of setNByoungmale()
		
		// setNBstability()
		// Calculate percent who lived in neighborhood for at least 1 year
		public void setNBstability( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalStable = 0;
			double avgPStable = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				if (a.getDurationRes() > 1) { totalStable += 1; }
			}
			// calculate percent who lived in neighborhood for at least 1 year
			if (totalAgentNum>0) {
				avgPStable = (double) totalStable/totalAgentNum;
			}
			nb.setPercStable(avgPStable);
		} // end of setNBstability()
		
		// setNBlightdrk()
		// Calculate percent light/moderate drinkers
		public void setNBlightdrk( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalLight = 0;
			double avgPLight = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				totalLight += a.getLightDrinker();
			}
			// calculate percent light/moderate drinkers in neighborhood, expressed as proportion
			if (totalAgentNum>0) {
				avgPLight = (double) totalLight/totalAgentNum;
			}
			nb.setPercLightDrk(avgPLight);
		} // end of setNBlightdrk
		
		// setNBheavydrk()
		// Calculate percent heavy drinkers
		public void setNBheavydrk( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalHeavy = 0;
			double avgPHeavy = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				totalHeavy += a.getHeavyDrinker();
			}
			// calculate percent heavy drinkers in neighborhood, expressed as proportion
			if (totalAgentNum>0) {
				avgPHeavy = (double) totalHeavy/totalAgentNum;
			}
			nb.setPercHeavyDrk(avgPHeavy);
		} // end of setNBheavydrk()
		
		// setNBperp()
		// Calculate average level of perpetration among agent residents
		public void setNBperp( AlcoholNeighborhood NB) {
			AlcoholNeighborhood nb = NB;
			int totalAgentNum = nb.getNeighborhoodAgentList().size();
			int totalAgentPerp = 0;
			double avgAgentPerp = -1;
			for (int n=0; n<totalAgentNum; n++) {
				AlcoholAgent a = nb.getNeighborhoodAgentList().get(n);
				totalAgentPerp += a.getViolperp();
			}
			// calculate rate of perpetration among agents in neighborhood, expressed as proportion
			if (totalAgentNum>0) {
				avgAgentPerp = (double) totalAgentPerp/totalAgentNum;
			}
			nb.setAvghoodperp(avgAgentPerp);
		} // end of setNBperp()
		
		
		// drinkingProb
		// Calculate drinking probabilities and identify those who are current light/moderate and heavy drinkers at baseline
		public void drinkingProb(AlcoholAgent a) {
							
			// calculate individual-level probabilities of being a current light/moderate or heavy drinker (from wtc data)
					
			// light drinker
			// 10.23.2014 -- calibration: increase intercept from 0.8013 to 1.00, decrease coefficient for black from -0.4489 to -0.55
			//							  increase coefficient for hisp from -0.1257 to -0.05
			double ilogitPLight = (double) 1.00 + (0.2366*a.gender) + (0.0459*a.age2) + (-0.5747*a.age3) +
											 (0.1098*a.age4) + (-0.3769*a.age5) + (-0.55*a.black) + (-0.05*a.hisp) + (-0.6482*a.otherRace) +
											 (-0.9647*a.hs) + (0.071*a.morehs) + (-0.1861*a.inc2) + (-0.0348*a.inc3) + (0.351*a.inc4);
			
			// heavy drinker
			// 10.23.2014 -- calibration: increase intercept from -1.0355 to -0.80, decrease coefficient for black from -0.6868 to -1.125
			//							  decrease coefficient for hisp from -0.1231 to -0.375, decrease coefficient for otherrace from -0.6285 to -0.88
			double ilogitPHeavy = (double) -0.80 + (0.6944*a.gender) + (0.7533*a.age2) + (0.4989*a.age3) +
											 (0.1481*a.age4) + (-1.1003*a.age5) + (-1.125*a.black) + (-0.375*a.hisp) + (-0.88*a.otherRace) +
											 (-0.2528*a.hs) + (-1.1088*a.morehs) + (-0.2107*a.inc2) + (-0.7835*a.inc3) + (-0.3612*a.inc4);
			
			double iProbLightDrk = (Math.exp(ilogitPLight)/(1 + Math.exp(ilogitPLight) + Math.exp(ilogitPHeavy)));
			double iProbHeavyDrk = (Math.exp(ilogitPHeavy)/(1 + Math.exp(ilogitPLight) + Math.exp(ilogitPHeavy)));
			
			// calculate neighborhood-level probabilities of being a current light/moderate or heavy drinker (from wtc data)
			AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.getAgenthood());
			
			// light drinker
			double nlogitPLight = (double) -2.4707 + (0.0196*nb.hoodinc1) + (0.0138*nb.hoodinc2) + 
								  (-0.1922*nb.percBlack) + (-0.0186*nb.percHisp) + (-0.8008*nb.avghoodviol) + 
								  (5.1653*nb.percLightDrk) + (2.5315*nb.percHeavyDrk);
			
			// heavy drinker
			double nlogitPHeavy = (double) -3.9946 + (0.7399*nb.hoodinc1) + (0.6614*nb.hoodinc2) + 
								  (-1.5819*nb.percBlack) + (-2.5387*nb.percHisp) + (-0.8384*nb.avghoodviol) + 
								  (2.7012*nb.percLightDrk) + (14.4055*nb.percHeavyDrk); 
			
			double nProbLightDrk = (Math.exp(nlogitPLight)/(1 + Math.exp(nlogitPLight) + Math.exp(nlogitPHeavy)));
			double nProbHeavyDrk = (Math.exp(nlogitPHeavy)/(1 + Math.exp(nlogitPLight) + Math.exp(nlogitPHeavy)));
			
			a.setProbLightDrk(((1 - alpha)*iProbLightDrk) + (alpha*nProbLightDrk));
			a.setProbHeavyDrk(((1 - alpha)*iProbHeavyDrk) + (alpha*nProbHeavyDrk));
			a.setProbNonDrk(1 - a.getProbLightDrk() - a.getProbHeavyDrk());
					
			// determine whether agent is a non-drinker, light/moderate drinker, or heavy drinker
			double randomProb = Random.uniform.nextDoubleFromTo(0,1);
			if (randomProb < a.getProbNonDrk()) { a.setDrinkStat(1); }
				else if (randomProb < (a.getProbNonDrk() + a.getProbLightDrk())) { a.setDrinkStat(2); }
				else { a.setDrinkStat(3); }
					
				if (a.getDrinkStat()==1) { a.setNonDrinker(1); } else { a.setNonDrinker(0); }
				if (a.getDrinkStat()==2) { a.setLightDrinker(1); } else { a.setLightDrinker(0); }
				if (a.getDrinkStat()==3) { a.setHeavyDrinker(1); } else { a.setHeavyDrinker(0); }
							
		} // end of drinkingProb()

		
		// drinkTypeUpdate
		// Update probability of drinking beer, wine, and spirits among current drinkers
		public void drinkTypeUpdate(AlcoholAgent a) {
					
			// based on individual-level characteristics (calculated from NESARC data)
					
			// beer
			double drinkBeer = (double) -2.017 + (1.378*a.gender) + (0.303*a.age2) + (0.215*a.age3) +
							   (0.280*a.age4) + (-0.039*a.age5) + (-0.348*a.age6) + (-0.004*a.black) + 
							   (-0.463*a.hisp) + (-0.293*a.otherRace) + (-0.019*a.hs) + (0.127*a.morehs) + 
							   (-0.043*a.inc2) + (-0.104*a.inc3) + (0.005*a.inc4) + (1.553*a.heavyDrinker);
			double probBeer = (Math.exp(drinkBeer)/(1 + Math.exp(drinkBeer)));
			a.setProbBeer(probBeer);
					
			// wine
			double drinkWine = (double) -2.494 + (-0.199*a.gender) + (0.165*a.age2) + (0.032*a.age3) +
							   (0.352*a.age4) + (0.470*a.age5) + (0.799*a.age6) + (-0.443*a.black) + 
							   (-0.424*a.hisp) + (-0.388*a.otherRace) + (0.181*a.hs) + (0.808*a.morehs) + 
							   (-0.038*a.inc2) + (0.076*a.inc3)+ (0.716*a.inc4) + (0.259*a.heavyDrinker);
			double probWine = (Math.exp(drinkWine)/(1 + Math.exp(drinkWine)));
			a.setProbWine(probWine);
					
			// spirits
			double drinkSpirit = (double) -2.997 + (0.520*a.gender)+ (-0.231*a.age2) + (-0.423*a.age3) +
								 (-0.245*a.age4) + (0.043*a.age5) + (0.495*a.age6) + (0.528*a.black) + 
								 (-0.625*a.hisp) + (-0.471*a.otherRace) + (0.287*a.hs) + (0.62*a.morehs) + 
								 (0.002*a.inc2) + (0.010*a.inc3) + (0.139*a.inc4) + (1.151*a.heavyDrinker);
			double probSpirit = (Math.exp(drinkSpirit)/(1 + Math.exp(drinkSpirit)));
			a.setProbSpirit(probSpirit);
					
			// determine whether agent prefers each beverage
			double randomBeer = Random.uniform.nextDoubleFromTo(0,1);
			double randomWine = Random.uniform.nextDoubleFromTo(0,1);
			double randomSpirit = Random.uniform.nextDoubleFromTo(0,1);
			if (randomBeer < a.getProbBeer()) { a.setPreferBeer(1); }
				else { a.setPreferBeer(0); }
			if (randomWine < a.getProbWine()) { a.setPreferWine(1); }
				else { a.setPreferWine(0); }
			if (randomSpirit < a.getProbSpirit()) { a.setPreferSpirit(1); }
				else { a.setPreferSpirit(0); }
					
		}
				
 
	
		
		// createSocialNetwork
		public void createSocialNetwork() {
			SocialNetworkList = new ArrayList<AlcoholAgent>(agentList); // add all agents to temporary social network list
			AlcoholNetwork baselineNetwork = new AlcoholNetwork();
			
			// Calibrating baseline social network characteristics
			System.out.println("Printed from build model");
			System.out.println("SocialNetworkList size:" + SocialNetworkList.size());
			System.out.println("AlcoholNetwork edges:" + baselineNetwork.getEdges());
			
			// Mean and range of number of friends
			// And number of agents who didn't meet target number of friends
			int sum = 0;
			int min = SocialNetworkList.size();
			int max = 0;
			int notenoughfriends = 0;
			int toomanyfriends = 0;
			int extrafriends = 0;
			for (int i=0; i < agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sum += a.friendList.size();
				if (a.friendList.size() > max) { max = a.friendList.size(); }
				if (a.friendList.size() < min) { min = a.friendList.size(); }
				if (a.friendList.size() < a.finalfriendsize) { notenoughfriends += 1; }
				if (a.friendList.size() > a.finalfriendsize) { toomanyfriends += 1; extrafriends += (a.friendList.size() - a.finalfriendsize); } 
			}
			int mean = sum / agentList.size();
			int meanextra = extrafriends / toomanyfriends;
			System.out.println("Mean number of friends:" + mean);
			System.out.println("Minimim number of friends:" + min);
			System.out.println("Maximum number of friends:" + max);
			System.out.println("Number of agents who did not meet target number of friends:" + notenoughfriends);
			System.out.println("Number of agents who had too many friends:" + toomanyfriends);
			System.out.println("Average number of extra friends:" + meanextra);
			
			/*
			/////////////////////////////////////// AGE DIFFERENCE BETWEEN FRIENDS
			// Mean and standard deviation of age difference between friends
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				// same age category and mean difference in ages between agent and friends
				int sameagecat = 0;
				double friendAgeSum = 0;
				for (int j=0; j<a.getTotalFriends(); j++) {
					friendAgeSum += a.friendList.get(j).getAge();
					if (a.friendList.get(j).getAgecat() == a.getAgecat()) { sameagecat += 1; }
				}
				double friendAgeMean = friendAgeSum / a.friendList.size();
				double friendSameAge = (double) sameagecat / a.friendList.size();
				a.setFriendAgeSum(friendAgeSum);
				a.setFriendAgeMean(friendAgeMean);
				a.setFriendAgeSame(friendSameAge);
				// calculations for standard deviation
				double friendAgeTemp = 0;
				for (int w=0; w<a.getTotalFriends(); w++) {
					friendAgeTemp += (a.friendList.get(w).getAge() - a.getFriendAgeMean()) * (a.friendList.get(w).getAge() - a.getFriendAgeMean());
				}
				a.setFriendAgeTemp(friendAgeTemp);
				double friendAgeDiff = Math.sqrt(friendAgeTemp/a.friendList.size());
				a.setFriendAgeDiff(friendAgeDiff);
			}
			double sumAgeDiff = 0;
			double sumAgeSame = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumAgeDiff += a.getFriendAgeDiff();
				sumAgeSame += a.getFriendAgeSame();
			}
			double meanAgeDiff = sumAgeDiff / agentList.size();
			double meanAgeSame = sumAgeSame / agentList.size();
			double sumAgeTemp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumAgeTemp += (a.getFriendAgeDiff() - meanAgeDiff) * (a.getFriendAgeDiff() - meanAgeDiff);
			}
			double sdAgeDiff = Math.sqrt(sumAgeTemp/agentList.size());
			System.out.println("Mean % friends in same age category: " + meanAgeSame);
			System.out.println("Mean friend age difference: " + meanAgeDiff);
			System.out.println("SD of friend age difference: " + sdAgeDiff);
			
			/////////////////////////////// EDUCATION DIFFERENCE BETWEEN FRIENDS
			// Mean and standard deviation of education difference between friends
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				// same education category and mean difference in education between agent and friends
				int sameeducat = 0;
				double friendEduSum = 0;
				for (int j=0; j<a.getTotalFriends(); j++) {
					friendEduSum += a.friendList.get(j).getEduYrs();
					if (a.friendList.get(j).getEducation() == a.getEducation()) { sameeducat += 1; }
				}
				double friendEduMean = friendEduSum / a.friendList.size();
				double friendSameEdu = (double) sameeducat / a.friendList.size();
				a.setFriendEduSum(friendEduSum);
				a.setFriendEduMean(friendEduMean);
				a.setFriendEduSame(friendSameEdu);
				// calculations for standard deviation
				double friendEduTemp = 0;
				for (int w=0; w<a.getTotalFriends(); w++) {
					friendEduTemp += (a.friendList.get(w).getEduYrs() - a.getFriendEduMean()) * (a.friendList.get(w).getEduYrs() - a.getFriendEduMean());
				}
				a.setFriendEduTemp(friendEduTemp);
				double friendEduDiff = Math.sqrt(friendEduTemp/a.friendList.size());
				a.setFriendEduDiff(friendEduDiff);
			}
			double sumEduDiff = 0;
			double sumEduSame = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumEduDiff += a.getFriendEduDiff();
				sumEduSame += a.getFriendEduSame();
			}
			double meanEduDiff = sumEduDiff / agentList.size();
			double meanEduSame = sumEduSame / agentList.size();
			double sumEduTemp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumEduTemp += (a.getFriendEduDiff() - meanEduDiff) * (a.getFriendEduDiff() - meanEduDiff);
			}
			double sdEduDiff = Math.sqrt(sumEduTemp/agentList.size());
			System.out.println("Mean % friends in same education category: " + meanEduSame);
			System.out.println("Mean friend education difference: " + meanEduDiff);
			System.out.println("SD of friend education difference: " + sdEduDiff);
			
			/////////////////////////////// GENDER DIFFERENCE BETWEEN FRIENDS
			// Index of qualitative variation (IQV)
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				// same gender as friends
				int samegender = 0;
				for (int j=0; j<a.getTotalFriends(); j++) {
					if (a.friendList.get(j).getGender() == a.getGender()) { samegender += 1; }
				}
				a.setFriendGenderSame((double) samegender / a.friendList.size());
				
				// calculations for IQV
				double friendGenderSum = Math.pow(a.getFriendGenderSame(), 2) + Math.pow(1 - (a.getFriendGenderSame()), 2);
				double friendGenderDiff = 2 * (1 - friendGenderSum);
				a.setFriendGenderDiff(friendGenderDiff);
			}
			double sumGenderDiff = 0;
			double sumGenderSame = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumGenderDiff += a.getFriendGenderDiff();
				sumGenderSame += a.getFriendGenderSame();
			}
			double meanGenderDiff = sumGenderDiff / agentList.size();
			double meanGenderSame = sumGenderSame / agentList.size();
			double sumGenderTemp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumGenderTemp += (a.getFriendGenderDiff() - meanGenderDiff) * (a.getFriendGenderDiff() - meanGenderDiff);
			}
			double sdGenderDiff = Math.sqrt(sumGenderTemp/agentList.size());
			System.out.println("Mean % friends in same gender category: " + meanGenderSame);
			System.out.println("Mean friend gender difference: " + meanGenderDiff);
			System.out.println("SD of friend gender difference: " + sdGenderDiff);
			
			/////////////////////////////// RACE DIFFERENCE BETWEEN FRIENDS
			// Index of qualitative variation (IQV)
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				// same race as friends
				int samerace = 0;
				for (int j=0; j<a.getTotalFriends(); j++) {
					if (a.friendList.get(j).getRace() == a.getRace()) { samerace += 1; }
				}
				a.setFriendRaceSame((double) samerace / a.friendList.size());

				// calculations for IQV
				double friendRaceSum = Math.pow(a.getFriendRaceSame(), 2) + Math.pow(1 - (a.getFriendRaceSame()), 2);
				double friendRaceDiff = (4/3) * (1 - friendRaceSum);
				a.setFriendRaceDiff(friendRaceDiff);
			}
			double sumRaceDiff = 0;
			double sumRaceSame = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumRaceDiff += a.getFriendRaceDiff();
				sumRaceSame += a.getFriendRaceSame();
			}
			double meanRaceDiff = sumRaceDiff / agentList.size();
			double meanRaceSame = sumRaceSame / agentList.size();
			double sumRaceTemp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				sumRaceTemp += (a.getFriendRaceDiff() - meanRaceDiff) * (a.getFriendRaceDiff() - meanRaceDiff);
			}
			double sdRaceDiff = Math.sqrt(sumRaceTemp/agentList.size());
			System.out.println("Mean % friends in same race category: " + meanRaceSame);
			System.out.println("Mean friend race difference: " + meanRaceDiff);
			System.out.println("SD of friend race difference: " + sdRaceDiff);
			*/
		}
		
		// agentDeath
		// Identify agents who die at current time step and notify their social network members
		// and replace with new agent, if recycling is desired
		public void agentDeath(AlcoholAgent a) {
					
			// Does agent die at this time step?
			double deathP = Random.uniform.nextDoubleFromTo(0,1);
			if (deathP <= a.getPMortality()) {
				a.setDied(1);	
			}
			
		} // end of agentDeath
			
		// resetDeath
		// Replace agents who died at last time step with new agent, if recycling is desired
		public void resetDeath( AlcoholAgent a) {
				
				// if agent will not be replaced, remove from all lists
				if (agentRecycle == 0) {
					agentList.remove(a);
					AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.getAgenthood());
					nb.neighborhoodAgentList.remove(a);
					nb.temphoodAgentList.remove(a);
				}
						
				// if agent recycling is allowed
				// replace deceased agent with a new 18-year-old agent
				if (agentRecycle == 1) {
					a.setAge(18);
					// reset dummy variables and trauma probabilities
					a.resetVars(getTickCount());
					// reset history of trauma and PTSD
					a.setPriorviolvict(0);
					a.setLastviolvict(0);
					a.setPriorviolperp(0);
					a.setLastviolperp(0);
					// reset initial substance use status
					a.setEverHeavyDrk(0);
					drinkingProb(a);
				}
				
				// Keep 'do not count' variable as 1
				a.setDoNotCount(1);
		
		} // end of resetDeath
		
		// closeOutlet
		// Close selected alcohol outlet as part of alcohol outlet intervention

		
		// closeEarly
		// Close selected alcohol outlet early as part of reduced hours intervention

		
		// drinkingTrans
		// Calculate drinking transition probabilities and identify those are current light/moderate and heavy drinkers
		// ALCOHOL TAXATION INTERVENTION ALSO OCCURS HERE WHEN IN EFFECT
		// AND EFFECTS OF EARLIER CLOSING TIMES ALSO OCCUR HERE WHEN IN EFFECT
		public void drinkingTrans(AlcoholAgent a) {
			
			AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.getAgenthood());
									
			/////////////////////////////// non-drinkers who become light/moderate drinkers
			
			if (a.getLastDrinkStat()==1) {
				
				// INFLUENCE OF INDIVIDUAL-LEVEL
				double ilogitP1 = (double) -0.5359 + (0.2904*a.gender) + (-0.0929*a.age2) + (-0.6975*a.age3) + 
								 (-0.4944*a.age4) + (-0.9467*a.age5) + (-1.0075*a.age6) +  (-0.4353*a.hs) + 
								 (-0.136*a.morehs) + (-0.5011*a.inc2) + (0.0282*a.inc3) + (0.4882*a.inc4) + 
								 (0.7624*a.lastviolvict);
				double iprobLight1 = (Math.exp(ilogitP1)/(1 + Math.exp(ilogitP1)));
				
				// INFLUENCE OF SOCIAL NETWORK AND DRINKERS AT PREFERRED OUTLET
				// adjust individual-level probability for influence of social network - adjusted prob will count for 15% of final prob
				double adjAbstainProb1 = Random.normal.nextDouble(0.11, 0.0153);
				double adjModProb1 = Random.normal.nextDouble(0.06, 0.0179);
				double iprobLight2 = ((( adjModProb1*a.numFrdLightDrk) - (adjAbstainProb1*a.numFrdNoDrk)) + 1)*iprobLight1;
				

				
				// INFLUENCE OF NEIGHBORHOOD
				double nlogitP1 = (double) -4.6586 + (-0.107*nb.hoodinc1) + (0.4702*nb.hoodinc2) + 
						  (3.1459*nb.avghoodviol) + (5.994*nb.percLightDrk) + (2.2168*nb.percHeavyDrk);
				double nprobLight1 = (Math.exp(nlogitP1)/(1 + Math.exp(nlogitP1)));
				
				// FINAL PROBABILITY
				double probLight1 = ((1 - alpha - network_alpha)*iprobLight1) + (network_alpha*iprobLight2) + (alpha*nprobLight1);
				
				double randomProb1 = Random.uniform.nextDoubleFromTo(0,1);
				if (randomProb1 < probLight1) { a.setDrinkStat(2); } else { a.setDrinkStat(1); }
			}
					
			////////////////////////////// light drinkers who become non-drinker or heavy drinkers
			if (a.getLastDrinkStat()==2) {
				
				///////////////////////////////// becoming non-drinker
				
				// INFLUENCE OF INDIVIDUAL LEVEL on becoming non-drinker
				double ilogitP2 = (double) -0.3288 + (-0.3634*a.gender) + (-0.1896*a.age2) + (-0.0468*a.age3) + 
								 (-0.3016*a.age4) + (0.1488*a.age5) + (0.0493*a.age6) + 
								 (-0.4643*a.hs) + (-0.8307*a.morehs) + (-0.3614*a.inc2) + (-0.7598*a.inc3) + 
								 (-1.2792*a.inc4) + (0.1279*a.lastviolvict);
				
				// INFLUENCE OF INDIVIDUAL LEVEL on becoming heavy drinker
				// 11.5.2014 -- calibration: change male coefficient from 0.6978 to -0.0842
				//				change lastviolvict from 0.3127 to 0.05
				double ilogitP3 = (double) -0.2061 + (-0.0842*a.gender) + (-1.0687*a.age2) + (-1.4939*a.age3) + 
								 (-2.2607*a.age4) + (-2.3726*a.age5) + (-2.8945*a.age6) + 
								 (0.1972*a.hs) + (-1.0469*a.morehs) + (-0.1372*a.inc2) + (-0.8733*a.inc3) + 
								 (-0.1993*a.inc4) + (0.05*a.lastviolvict);
				
				// calculation of individual-level probabilities
				double iprobNonDrk = (Math.exp(ilogitP2)/(1 + Math.exp(ilogitP2) + Math.exp(ilogitP3)));
				double iprobHeavy = (Math.exp(ilogitP3)/(1 + Math.exp(ilogitP2) + Math.exp(ilogitP3)));
				
				// INFLUENCE OF SOCIAL NETWORK AND DRINKERS AT PREFERRED OUTLET on becoming non-drinker
				double adjAbstainProb2 = Random.normal.nextDouble(0.22, 0.0281);
				double adjModProb2 = Random.normal.nextDouble(0.05, 0.0179);
				double adjHeavyProb2 = Random.normal.nextDouble(0.07, 0.0255);
				double iprobNonDrk2 = (((adjAbstainProb2*a.numFrdNoDrk) - ( adjModProb2*a.numFrdLightDrk) - (adjHeavyProb2*a.numFrdHeavyDrk)) + 1)*iprobNonDrk;
				
				// INFLUENCE OF SOCIAL NETWORK AND DRINKERS AT PREFERRED OUTLET on becoming heavy drinker
				double adjAbstainProb3 = Random.normal.nextDouble(0.10, 0.0281);
				double adjHeavyProb3 = Random.normal.nextDouble(0.18, 0.0357);
				double iprobHeavy2 = (((adjHeavyProb3*a.numFrdHeavyDrk) - (adjAbstainProb3*a.numFrdNoDrk)) + 1)*iprobHeavy;
				

				
				// INFLUENCE OF NEIGHBORHOOD on becoming non-drinker
				double nlogitP2 = (double) 0.3414 + (-0.4257*nb.hoodinc1) + (-0.4381*nb.hoodinc2) + 
						  (3.2196*nb.avghoodviol) + (-3.532*nb.percLightDrk) + (0.833*nb.percHeavyDrk);
						
				// INFLUENCE OF NEIGHBORHOOD on becoming heavy drinker
				// 12.4.2014 -- added racial composition variables as predictors
				//				calibration: increase coefficient for percBlack from -1.8849 to 1.25
				double nlogitP3 = (double) -2.28 + (-0.2088*nb.hoodinc1) + (-0.0903*nb.hoodinc2) + 
						  (1.9665*nb.avghoodviol) + (-2.5772*nb.percLightDrk) + (13.8191*nb.percHeavyDrk) +
						  (1.25*nb.percBlack)+ (1.7474*nb.percHisp);
				
				// calculation of neighborhood-level probabilities
				double nprobNonDrk = (Math.exp(nlogitP2)/(1 + Math.exp(nlogitP2) + Math.exp(nlogitP3)));
				double nprobHeavy = (Math.exp(nlogitP3)/(1 + Math.exp(nlogitP2) + Math.exp(nlogitP3)));
				
				// FINAL PROBABILITIES
				double probNonDrk = ((1 - alpha - network_alpha)*iprobNonDrk) + (network_alpha*iprobNonDrk2) + (alpha*nprobNonDrk);
				double probHeavy = ((1 - alpha - network_alpha)*iprobHeavy) + (network_alpha*iprobHeavy2) + (alpha*nprobHeavy);
				
					// MODIFY PROBABILITY OF LIGHT DRINKER BECOMING HEAVY DRINKER IF ALCOHOL TAXATION INTERVENTION IN EFFECT
					if (intervention == 6 && intTarget == 0 && getTickCount()>startAging+1) {
						double decreaseProb = 0;
						if (a.getPreferBeer()==0 && a.getPreferWine()==0 && a.getPreferSpirit()==0) {
							decreaseProb = intChange*0.53;
							if (a.getHouseincome()==1) { decreaseProb = decreaseProb*1.6; }
							if (a.getHouseincome()==2) { decreaseProb = decreaseProb*1.08; }
							if (a.getHouseincome()==3) { decreaseProb = decreaseProb*0.76; }
							if (a.getHouseincome()==4) { decreaseProb = decreaseProb*0.27; }
						}
						if (a.getPreferBeer()==1 || a.getPreferWine()==1 || a.getPreferSpirit()==1) {
							decreaseProb = intChange*0.53*0.50;
							if (a.getHouseincome()==1) { decreaseProb = decreaseProb*1.6; }
							if (a.getHouseincome()==2) { decreaseProb = decreaseProb*1.08; }
							if (a.getHouseincome()==3) { decreaseProb = decreaseProb*0.76; }
							if (a.getHouseincome()==4) { decreaseProb = decreaseProb*0.27; }
						}
						probHeavy = probHeavy - (probHeavy*decreaseProb);
					}
					if (intervention == 6 && intTarget == 1 && getTickCount()>startAging+1) {
						double decreaseProb = 0;
						if (a.getAnyBeer() == 1 && a.getPreferBeer()==0) {
							decreaseProb = intChange*0.53;
							if (a.getHouseincome()==1) { decreaseProb = decreaseProb*1.6; }
							if (a.getHouseincome()==2) { decreaseProb = decreaseProb*1.08; }
							if (a.getHouseincome()==3) { decreaseProb = decreaseProb*0.76; }
							if (a.getHouseincome()==4) { decreaseProb = decreaseProb*0.27; }	
						}
						if (a.getAnyBeer()==1 && a.getPreferBeer()==1) {
							decreaseProb = intChange*0.53*0.50;
							if (a.getHouseincome()==1) { decreaseProb = decreaseProb*1.6; }
							if (a.getHouseincome()==2) { decreaseProb = decreaseProb*1.08; }
							if (a.getHouseincome()==3) { decreaseProb = decreaseProb*0.76; }
							if (a.getHouseincome()==4) { decreaseProb = decreaseProb*0.27; }	
						}
						probHeavy = probHeavy - (probHeavy*decreaseProb);
					}
					
					// MODIFY PROBABILITY OF LIGHT DRINKER BECOMING HEAVY DRINKER IF EARLIER CLOSING TIMES AT OUTLET
					if ((intervention == 7 || intervention == 10)) {
						double decreaseProb = Random.normal.nextDouble(0.037, 0.01);
						probHeavy = probHeavy - (probHeavy*decreaseProb);
					}
				double probLight2 = 1 - probNonDrk - probHeavy;
				
				double randomProb2 = Random.uniform.nextDoubleFromTo(0,1);
				if (randomProb2 < probLight2) { a.setDrinkStat(2); }
					else if (randomProb2 < (probLight2 + probNonDrk)) { a.setDrinkStat(1); }
					else { a.setDrinkStat(3); }
			}
					
			///////////////////////////////// heavy drinkers who become light/moderate drinkers
			if (a.getLastDrinkStat()==3) {
				
				// INFLUENCE OF INDIVIDUAL LEVEL
				// 11.6.2014 -- calibration: include interaction between male gender and violent victimization
				//				increase intercept from -1.1757 to -0.35, increase lastviolvict from -1.2021 to -0.35
				//				reduce morehs coefficient from 2.0543 to 0.35
				//				reduce gender coefficient from -0.2719 to -0.35
				//				reduce age2 coefficient from -0.1278 to -0.075
				//				reduce age4 coefficient from 0.985 to -0.15
				//				reduce hs coefficient from 0.3849 to 0.15
				// 				reduce morehs coefficient from 0.35 to 0.13
				//				increase inc2 coefficient from -0.1163 to -0.05
				//				reduce inc3 coefficient from -0.0219 to -0.10
				//				reduce inc4 coefficient from -0.0633 to -0.1175 to -0.15
				//				reduce interaction coefficient from 14.9931 to 8.75
				double ilogitP4 = (double) 0.35 + (-0.35*a.gender) + (-0.075*a.age2) + 
					 			 (-0.6443*a.age3) + (-0.15*a.age4) + (-1.0192*a.age5) + (-0.5927*a.age6) + 
								 (0.15*a.hs) + (0.13*a.morehs) + (-0.05*a.inc2) + (-0.10*a.inc3) + 
								 (-0.15*a.inc4) + (-0.35*a.lastviolvict) + (8.75*a.gender*a.lastviolvict);
				// 11.20.2014 - revised equation to change referent groups
				// 				calibration: increase intercept from 0.2707 to 0.45
				//				increase lesshs coefficient from -1.8542 to -1.35
				//				increase hs coefficient from -1.4524 to -1.00
				// double ilogitP4 = (double) 0.45 + (-0.4162*a.gender) + (0.483*a.age1) +
					//			  (0.4421*a.age2) + (-0.2216*a.age3) + (1.4761*a.age4) + (0.3926*a.age5) +
					//			  (-1.35*a.lesshs) + (-1.400*a.hs) + (-0.0142*a.inc1) + (0.1136*a.inc2) +
					//			  (0.0661*a.inc3) + (-1.3622*a.lastviolvict) + (15.2391*a.gender*a.lastviolvict);
				double iprobLight3 = (Math.exp(ilogitP4)/(1 + Math.exp(ilogitP4)));
				
				// INFLUENCE OF SOCIAL NETWORK AND DRINKERS AT PREFERRED OUTLET
				// adjust individual-level probability for influence of social network
				double adjAbstainProb4 = Random.normal.nextDouble(0.11, 0.0153);
				double adjModProb4 = Random.normal.nextDouble(0.06, 0.0179);
				double iprobLight4 = ((( adjModProb4*a.numFrdLightDrk) - (adjAbstainProb4*a.numFrdNoDrk)) + 1)*iprobLight3;
				// 3.27.2014 -- probability also influenced by proportion of heavy drinkers at associated outlet
				if (a.getMyOutlet() != null) { 
					AlcoholOutlet myOutlet = (AlcoholOutlet)a.getMyOutlet();
					// decrease probability by 25% if 50% heavy drinkers at outlet
					if (myOutlet.getPLightDrk() >= 0.50) { iprobLight4 = iprobLight4 - (0.25*iprobLight4); }
				}
				
				// INFLUENCE OF NEIGHBORHOOD LEVEL
				// 12.4.2014 -- included racial composition variables as predictors
				//				calibration: increase coefficient for percBlack from 1.7344 to 6.5
				//							 increase intercept from 0.8926 to 1.10
				double nlogitP4 = (double) 1.20 + (-1.0433*nb.hoodinc1) + (-1.906*nb.hoodinc2) + 
								  (-7.5086*nb.avghoodviol) + (1.0108*nb.percLightDrk) + (-16.456*nb.percHeavyDrk) +
								  (6.5*nb.percBlack) + (6.7193*nb.percHisp);
				// double nlogitP4 = (double) -1.9461 + (-0.4184*nb.hoodinc1) + (-0.0205*nb.hoodinc2) +
					//					   (-2.342*nb.avghoodviol) + (-2.9454*nb.percLightDrk) + 
					//					   (14.6423*nb.percHeavyDrk) + (-3.5637*nb.percBlack) +
					//					   (2.0063*nb.percHisp) + (23.6114*nb.avghoodviol*nb.percBlack);
				double nprobLight3 = (Math.exp(nlogitP4)/(1 + Math.exp(nlogitP4)));
				
				// FINAL PROBABILITY
				double probLight3 = ((1 - alpha - network_alpha)*iprobLight3) + (network_alpha*iprobLight4) + (alpha*nprobLight3);
				
				// MODIFY PROBABILITY OF HEAVY DRINKER BECOMING LIGHT DRINKER IF ALCOHOL TAXATION INTERVENTION IN EFFECT
				if (intervention == 6 && intTarget == 0 && getTickCount()>startAging+1) {
					double increaseProb = 0;
					if (a.getPreferBeer()==0 && a.getPreferWine()==0 && a.getPreferSpirit()==0) {
						increaseProb = intChange*0.53;
						if (a.getHouseincome()==1) { increaseProb = increaseProb*1.6; }
						if (a.getHouseincome()==2) { increaseProb = increaseProb*1.08; }
						if (a.getHouseincome()==3) { increaseProb = increaseProb*0.76; }
						if (a.getHouseincome()==4) { increaseProb = increaseProb*0.27; }
					}
					if (a.getPreferBeer()==1 || a.getPreferWine()==1 || a.getPreferSpirit()==1) {
						increaseProb = intChange*0.53*0.50;
						if (a.getHouseincome()==1) { increaseProb = increaseProb*1.6; }
						if (a.getHouseincome()==2) { increaseProb = increaseProb*1.08; }
						if (a.getHouseincome()==3) { increaseProb = increaseProb*0.76; }
						if (a.getHouseincome()==4) { increaseProb = increaseProb*0.27; }
					}
					probLight3 = probLight3 + (probLight3*increaseProb);
				}
				if (intervention == 6 && intTarget == 1 && getTickCount()>startAging+1) {
					double increaseProb = 0;
					if (a.getAnyBeer() == 1 && a.getPreferBeer()==0) {
						increaseProb = intChange*0.53;
						if (a.getHouseincome()==1) { increaseProb = increaseProb*1.6; }
						if (a.getHouseincome()==2) { increaseProb = increaseProb*1.08; }
						if (a.getHouseincome()==3) { increaseProb = increaseProb*0.76; }
						if (a.getHouseincome()==4) { increaseProb = increaseProb*0.27; }	
					}
					if (a.getAnyBeer()==1 && a.getPreferBeer()==1) {
						increaseProb = intChange*0.53*0.50;
						if (a.getHouseincome()==1) { increaseProb = increaseProb*1.6; }
						if (a.getHouseincome()==2) { increaseProb = increaseProb*1.08; }
						if (a.getHouseincome()==3) { increaseProb = increaseProb*0.76; }
						if (a.getHouseincome()==4) { increaseProb = increaseProb*0.27; }	
					}
					probLight3 = probLight3 + (probLight3*increaseProb);
				}
				// MODIFY PROBABILITY OF HEAVY DRINKER BECOMING LIGHT DRINKER IF EARLIER CLOSING TIMES AT OUTLET
				if ((intervention == 7 || intervention == 10) && a.getOutletEarly()==1) {
					double increaseProb = Random.normal.nextDouble(0.037,0.01);
					probLight3 = probLight3 + (probLight3*increaseProb);
				}
				
				double randomProb3 = Random.uniform.nextDoubleFromTo(0,1);
				if (randomProb3 < probLight3) { a.setDrinkStat(2); } else { a.setDrinkStat(3); }
			}
					
			// update dummy variables for current drinking status
			if (a.getDrinkStat()==1) { a.setNonDrinker(1); } else { a.setNonDrinker(0); }
			if (a.getDrinkStat()==2) { a.setLightDrinker(1); } else { a.setLightDrinker(0); }
			if (a.getDrinkStat()==3) { a.setHeavyDrinker(1); } else { a.setHeavyDrinker(0); }
									
		} // end of drinkingTrans()
		
		// findViolOutlets()
		// Create list of nearby and preferred alcohol outlets
		// And note whether violent events occurred at these outlets at the last time step

				
		
		// findVictims()
		// Identify true victims of non-fatal violence and homicide and true perpetrators at each time step
		public void findVictims( AlcoholAgent a) {
			int lookDistance = getLookForVictims();
			// list of cells within range of perpetrator
			Vector neighbors = hoodSpace.getMooreNeighbors(a.getX(), a.getY(), lookDistance, lookDistance, false);
			// do any cells contain potential victims?
			// if so, and they have not already been assaulted by someone else
			// they will be this perpetrator's victims
			// note that each perpetrator can have multiple victims
			// but each victim can have only one perpetrator
			// 4.16.2014 -- if police officer is present near potential victim, violent act is prevented
			for (int i=0; i<neighbors.size(); i++) {
				AlcoholCell nextCell = (AlcoholCell)neighbors.elementAt(i);
				if (nextCell.getPotVictim() == 1 & nextCell.getRealVictim() == 0) {
					// is a police officer nearby?
					int violPrevented = 0;

					// is a violence interrupter nearby?

					// if no police officer or violence interrupter is nearby, violent act occurs
					if (violPrevented == 0) {
						// confirm perpetrator status of index agent
						a.setViolperp(1);
						// confirm that there is a true victim in this cell
						nextCell.setRealVictim(1);
						// confirm victim status of agent located in cell
						AlcoholAgent victim = nextCell.getMyAgent();
						if (victim.getPothomicide() == 1) { victim.setHomicide(1); }
						else if (victim.getPotviolvict() == 1) { victim.setViolvict(1); }
						// alcohol-related violence if either perpetrator or victim is heavy drinker
						if (victim.getPothomicide() == 1 & (victim.getHeavyDrinker() == 1 || a.getHeavyDrinker() == 1)) { victim.setAlcHomicide(1); }
						if (victim.getPotviolvict() == 1 & (victim.getHeavyDrinker() == 1 || a.getHeavyDrinker() == 1)) { victim.setAlcViol(1); }
						// add violent event to number of violent events in victim's neighborhood
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(victim.getAgenthood());
						nb.setNumviolevent(nb.getNumviolevent() + 1);
					}
				}
			} 
		} // end of findVictims()
		
		// countViolence()
		// Calculate number of violent incidents that took place in close proximity to each outlet 
					
		////////////////////////////////////////////// FUNCTIONS TO CALCULATE PERCENTAGES FOR REAL-TIME GRAPHS
		
		public double getPercHeavyDrk() {
			numHeavyDrk = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numHeavyDrk += a.getHeavyDrinker();
			}
			if (agentList.size()>1) {
				percHeavyDrk = (numHeavyDrk / (double)agentList.size())*100;
			}
			return percHeavyDrk;
		}
		
		public double getPercViolvict() {
			numViolvict = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numViolvict += a.getViolvict();
			}
			if (agentList.size()>1) {
				percViolvict = (numViolvict / (double)agentList.size())*100;
			}	
			return percViolvict;
		}
		
		public double getPercPriorviolvict() {
			numPriorviolvict = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numPriorviolvict += a.getPriorviolvict();
			}
			if (agentList.size()>1) {
				percPriorviolvict = (numPriorviolvict / (double)agentList.size())*100;
			}
			return percPriorviolvict;
		}
		
		public double getPercViolperp() {
			numViolperp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numViolperp += a.getViolperp();
			}
			if (agentList.size()>1) {
				percViolperp = (numViolperp / (double)agentList.size())*100;
			}
			return percViolperp;
		}
		
		public double getPercPriorviolperp() {
			numPriorviolperp = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numPriorviolperp += a.getPriorviolperp();
			}
			if (agentList.size()>1) {
				percPriorviolperp = (numPriorviolperp / (double)agentList.size())*100;
			}
			return percPriorviolperp;
		}
		
		public double getPercDied() {
			numDied = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numDied += a.getDied();
			}
			if (agentList.size()>1) {
				percDied = (numDied / (double)agentList.size())*100;
			}
			return percDied;
		}
		
		public double getPercMoved() {
			numMoved = 0;
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				numMoved += a.getMoved();
			}
			if (agentList.size()>1) {
				percMoved = (numMoved / (double)agentList.size())*100;
			}
			return percMoved;
		}
		
		////////////////////////////////////////////////////////// FUNCTIONS USED TO CLASSIFY NEIGHBORHOODS
		
		// Calculate average household income across neighborhoods
		public double getAvgHoodinc() {
			avgHoodinc = 0.0;
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				avgHoodinc += nb.getAvghoodinc();
			}
			if (hoodList.size()>1) {
				avgHoodinc /= (double) hoodList.size();
			}
			return avgHoodinc;
		}
		
		// Calculate average level of violence across neighborhoods
		public double getAvgHoodviol() {
			avgHoodviol = 0.0;
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				avgHoodviol += nb.getAvghoodviol();
			}
			if (hoodList.size()>1) {
				avgHoodviol /= (double) hoodList.size();
			}
			return avgHoodviol;
		}
		
		// Calculate average level of residential stability across neighborhoods
		public double getAvgStable() {
			avgHoodstable = 0.0;
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				avgHoodstable += nb.getPercStable();
			}
			if (hoodList.size()>1) {
				avgHoodstable /= (double) hoodList.size();
			}
			return avgHoodstable;
		}
		
		// Calculate average level of heavy drinking across neighborhoods
		public double getAvgHoodheavy() {
			avgHoodheavy = 0.0;
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				avgHoodheavy += nb.getPercHeavyDrk();
			}
			if (hoodList.size()>1) {
				avgHoodheavy /= (double) hoodList.size();
			}
			return avgHoodheavy;
		}
				
		////////////////////////////////////FUNCTIONS FOR AGENT STEP REPORT
		
		// Start agent step report
		public PrintWriter startAgentStepReportFile() {
			System.out.println( "Agent Step Report started" );
			agentStepReportFile = null;
			agentStepReportFile = IOUtils.openFileToWrite( outputDirName, agentStepReportFileName, "r");
			writeLineToStepReportFile( "# begin report file" );
			return agentStepReportFile;
		}
		
		// End agent step report
		public void endStepReportFile() {
			writeLineToStepReportFile( "# end report file" );
		}
		
		// Write each line to agent step report
		public void writeLineToStepReportFile( String line) {
			agentStepReportFile.println( line );
		}
		
		// Agent step report
		// Outputs specified characteristics for each agent at each time step
		// This will only be used to check that all variables are being calculated and set correctly
		public void stepReport() {
			String s;
			System.out.printf( "==> Agent stepReport %.0f:\n", getTickCount() );
			
			// agent characteristics of interest
			for (int i=0; i<agentList.size(); i++) {
				AlcoholAgent a = (AlcoholAgent)agentList.get(i);
				double agentID = (double) a.getID();
				double agentX = (double) a.getX();
				double agentY = (double) a.getY();
				double agentHood = (double) a.getAgenthood();
				double age = (double) a.getAge();
				double age2 = (double) a.getAge2();
				double age3 = (double) a.getAge3();
				double age4 = (double) a.getAge4();
				double age5 = (double) a.getAge5();
				double gender = (double) a.getGender();
				double race = (double) a.getRace();
				double black = (double) a.getBlack();
				double hisp = (double) a.getHisp();
				double otherRace = (double) a.getOtherRace();
				double education = (double) a.getEducation();
				double hs = (double) a.getHs();
				double morehs = (double) a.getMorehs();
				double houseincome = (double) a.getHouseincome();
				double inc2 = (double) a.getInc2();
				double inc3 = (double) a.getInc3();
				double inc4 = (double) a.getInc4();
				double died = (double) a.getDied();
				double baseinc16 = (double) a.getBaseinc16();
				double pviolvict = (double) a.getPviolvict();
				double potviolvict = (double) a.getPotviolvict();
				double violvict = (double) a.getViolvict();
				double pviolperp = (double) a.getPviolperp();
				double potviolperp = (double) a.getPotviolperp();
				double violperp = (double) a.getViolperp();
				double lastviolvict = (double) a.getLastviolvict();
				double priorviolvict = (double) a.getPriorviolvict();
				double priorviolperp = (double) a.getPriorviolperp();
				double lastviolperp = (double) a.getLastviolperp();
				double probnondrk = (double) a.getProbNonDrk();
				double problightdrk = (double) a.getProbLightDrk();
				double probheavydrk = (double) a.getProbHeavyDrk();
				double lastdrinkstat = (double) a.getLastDrinkStat();
				double drinkstat = (double) a.getDrinkStat();
				double nondrinker = (double) a.getNonDrinker();
				double lightdrinker = (double) a.getLightDrinker();
				double heavydrinker = (double) a.getHeavyDrinker();
				double alcviol = (double) a.getAlcViol();
				double probhom = (double) a.getProbHomicide();
				double homicide = (double) a.getHomicide();
				double alchom = (double) a.getAlcHomicide();
				double probmove = (double) a.getPMove();
				double moved = (double) a.getMoved();
				double duration = (double) a.getDurationRes();
				double dur1 = (double) a.getDurRes1();
				double dur2 = (double) a.getDurRes2();
				double dur3 = (double) a.getDurRes3();
				double everhigh = (double) a.getEverHighInc();
				double everlow = (double) a.getEverLowInc();
				double baseinchood = (double) a.getBaseIncHood();
				double assignfrd = (double) a.getFinalfriendsize();
				double numfrd = (double) a.friendList.size();
				double nodrkfrd = (double) a.getNumFrdNoDrk();
				double moddrkfrd = (double) a.getNumFrdLightDrk();
				double heavydrkfrd = (double) a.getNumFrdHeavyDrk();
				double friend1id = (double) a.friendList.get(0).ID;
				double friend2id = (double) 99999; if (a.friendList.size() >= 2) { friend2id = (double) a.friendList.get(1).ID; } 
				double friend3id = (double) 99999; if (a.friendList.size() >= 3) { friend3id = (double) a.friendList.get(2).ID; } 
				double friend4id = (double) 99999; if (a.friendList.size() >= 4) { friend4id = (double) a.friendList.get(3).ID; }
				double friend5id = (double) 99999; if (a.friendList.size() >= 5) { friend5id = (double) a.friendList.get(4).ID; }
				double friend6id = (double) 99999; if (a.friendList.size() >= 6) { friend6id = (double) a.friendList.get(5).ID; }
				double friend7id = (double) 99999; if (a.friendList.size() >= 7) { friend7id = (double) a.friendList.get(6).ID; }
				double friend8id = (double) 99999; if (a.friendList.size() >= 8) { friend8id = (double) a.friendList.get(7).ID; }
				double friend9id = (double) 99999; if (a.friendList.size() >= 9) { friend9id = (double) a.friendList.get(8).ID; }
				double closeearly = (double) a.getOutletEarly();
				
				s = String.format( "%5.0f ", getTickCount() );
				s += String.format( "%5.0f %5.0f %5.0f %5.0f ", agentID, agentX, agentY, agentHood);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f ", age, age2, age3, age4, age5);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f ", gender, race, black, hisp, otherRace);
				s += String.format( "%5.0f %5.0f %5.0f ", education, hs, morehs);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f ", baseinc16, houseincome, inc2, inc3, inc4);
				s += String.format( "%5.0f %5.3f %5.0f %5.0f %5.0f %5.0f ", died, pviolvict, potviolvict, violvict, lastviolvict, priorviolvict);
				s += String.format( "%5.3f %5.0f %5.0f %5.0f %5.0f ", pviolperp, potviolperp, violperp, lastviolperp, priorviolperp);
				s += String.format( "%5.3f %5.3f %5.3f ", probnondrk, problightdrk, probheavydrk);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f ", lastdrinkstat, drinkstat, nondrinker, lightdrinker, heavydrinker);
				s += String.format( "%5.0f %5.3f %5.0f %5.0f ", alcviol, probhom, homicide, alchom);
				s += String.format( "%5.3f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f ", probmove, moved, duration, dur1, dur2, dur3, everhigh, everlow, baseinchood);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f ", assignfrd, numfrd, nodrkfrd, moddrkfrd, heavydrkfrd);
				s += String.format( "%5.0f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f %5.0f ", friend1id, friend2id, friend3id, friend4id, friend5id, friend6id, friend7id, friend8id, friend9id);
				s += String.format( "%5.0f ", closeearly);
				
				writeLineToStepReportFile( s );
				agentStepReportFile.flush();
			}
		} // end of stepReport
		
		/////////////////////////////////// FUNCTIONS FOR NEIGHBORHOOD STEP REPORT
		
		// Start neighborhood step report
		public PrintWriter startHoodStepReportFile ( ) {
			System.out.println( "Hood Step Report started" );
			hoodStepReportFile = null;
			hoodStepReportFile = IOUtils.openFileToWrite( outputDirName, hoodStepReportFileName, "r");
			writeLineToNBStepReportFile( "# begin reportfile" );
			return hoodStepReportFile;
		}
		
		// End neighborhood step report
		public void endNBStepReportFile ( ) {
			writeLineToNBStepReportFile( "# end report file" );
		}
		
		// Write each line to neighborhood step report
		public void writeLineToNBStepReportFile( String line) {
			hoodStepReportFile.println( line );
		}

		// Neighborhood step report
		// Outputs specified characteristics for each neighborhood at each time step
		// This will only be used to check that all variables are being calculated and set correctly
		public void hoodStepReport() {
			String s; 
			System.out.printf( "==> Neighborhood stepReport %.0f:\n", getTickCount() );

				// neighborhood characteristics of interest
				for ( int t=0; t<hoodList.size(); t++ ){	
					AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
					double hoodID = (double) nb.getID();
					double avghoodinc = (double) nb.getAvghoodinc();
					double lastavghoodinc = (double) nb.getLastavghoodinc();
					double changeinc = (double) nb.getChangeIncome();
					double hoodinc = (double) nb.getHoodinc();
					double hoodinc1 = (double) nb.getHoodinc1();
					double hoodinc2 = (double) nb.getHoodinc2();
					double highhoodinc = (double) nb.getHighhoodinc();
					double avghoodviol = (double) nb.getAvghoodviol();
					double lastavghoodviol = (double) nb.getLastavghoodviol();
					double changeviol = (double) nb.getChangeViol();
					double highhoodviol = (double) nb.getHighhoodviol();
					double avghoodperp = (double) nb.getAvghoodperp();
					double targethood = (double) nb.getTargetHood();
					double pblack = (double) nb.getPercBlack();
					double phisp = (double) nb.getPercHisp();
					double pstable = (double) nb.getPercStable();
					double police = (double) nb.getNumPolice();
					double plight = (double) nb.getPercLightDrk();
					double pheavy = (double) nb.getPercHeavyDrk();
					double avgage = (double) nb.getAvgAge();
					double homrate = (double) nb.getHomrate();
					double alchomrate = (double) nb.getAlchomrate();
					double numagent = (double) nb.getNeighborhoodAgentList().size();
					double numcell = (double) nb.getNeighborhoodCellList().size();
					
					s = String.format( "%5.0f ", getTickCount() );
					s += String.format( "%5.0f %5.3f %5.3f %5.3f ", hoodID, avghoodinc, lastavghoodinc, changeinc); 
					s += String.format( "%5.0f %5.0f %5.0f %5.0f ", hoodinc, hoodinc1, hoodinc2, highhoodinc);
					s += String.format( "%5.3f %5.3f %5.3f %5.0f ", avghoodviol, lastavghoodviol, changeviol, highhoodviol);
					s += String.format( "%5.3f %5.0f %5.3f %5.3f %5.3f ", avghoodperp, targethood, pblack, phisp, pstable);
					s += String.format( "%5.0f %5.3f %5.3f %5.3f ", police, plight, pheavy, avgage);
					s += String.format( "%5.3f %5.3f %5.0f %5.3f %5.3f %5.3f %5.0f ", homrate, alchomrate, numagent, numcell);
					writeLineToNBStepReportFile( s );
					hoodStepReportFile.flush();
				}
				
		} // end of hoodStepReport	
		
		/////////////////////////////////// FUNCTIONS FOR ALCOHOL OUTLET STEP REPORT
		
		// Start outlet step report


		// End outlet step report

		// Write each line to outlet step report


		// Outlet step report
		// Outputs specified characteristics for each alcohol outlet at each time step
		// This will only be used to check that all variables are being calculated and set correctly

		


		
		/////////////////////////// SETTERS AND GETTERS
		
		public Schedule getSchedule() {return schedule;}
		
		public int getNumAgents() { return numAgents; }
		public void setNumAgents(int numAgents) { 
			this.numAgents = numAgents;
			if (  schedule != null ) {
				System.err.printf("\nCan't change numAgents mid-run.\n");
				System.err.printf("\nChange will not take effect until re-init.\n");
			}
		}
		
		public int getWorldXsize() { return worldXsize; }
		public void setWorldXsize(int wxs) { this.worldXsize = wxs; }
		
		public int getWorldYsize() { return worldYsize; }
		public void setWorldYsize(int wys) { this.worldYsize = wys; }
		
		public int getNumHoods() { return numHoods; }
		public void setNumHoods(int nh) { this.numHoods = nh; }
				
		public int getStartAging() { return startAging; }
		public void setStartAging(int sa) { this.startAging = sa; }
		
		public int getStopModelRun() { return stopModelRun; }
		public void setStopModelRun(int smr) { this.stopModelRun = smr; }
		
		public int getDisplayGUI() { return displayGUI; }
		public void setDisplayGUI( int d) { this.displayGUI=d; }
		
		public int getLookForVictims() { return lookForVictims; }
		public void setLookForVictims(int lfv) { this.lookForVictims = lfv; }
		
		public double getAlpha() { return alpha; }
		public void setAlpha(double a) { this.alpha = a; }
		
		public double getNetwork_alpha() { return network_alpha; }
		public void setNetwork_alpha(double a) { this.network_alpha = a; }
		
		public int getOutputAgentSteps() { return outputAgentSteps; }
		public void setOutputAgentSteps( int oas) { this.outputAgentSteps = oas; }
		
		public int getOutputHoodSteps() { return outputHoodSteps; }
		public void setOutputHoodSteps( int ohs) { this.outputHoodSteps = ohs; }
		
		public int getAllowDeath() { return allowDeath; }
		public void setAllowDeath( int i) { this.allowDeath=i; }
		
		public int getAgentRecycle() { return agentRecycle; }
		public void setAgentRecycle( int i) { this.agentRecycle=i; }
		
		public int getIntervention() { return intervention; }
		public void setIntervention( int i) { this.intervention=i; }
		
		public int getIntTarget() { return intTarget; }
		public void setIntTarget( int i) { this.intTarget=i; }
		
		public double getIntChange() { return intChange; }
		public void setIntChange( double i) { this.intChange=i; }
		
		public int getIntDuration() { return intDuration; }
		public void setIntDuration( int i) { this.intDuration=i; }
		
		

		

		
		public int getNumOutreach() { return numOutreach; }
		public void setNumOutreach( int i) { this.numOutreach = i; }
		

		
		////////////////////////////////RECORD SUMMARY RESULTS AT EACH TIME STEP

		// Make the data recorder that writes file output
		// Outputs summary measures across agents at each time step
		public void recordOutput() {
			
		// String	recorderName = "alcohol-oct-baselineagent.txt";	
		String 	recorderName = "alcohol-final" + "-intervention-" + (int)(intervention) + "-intTarget-" + (int)(intTarget) + "-intChange-" + (int)(intChange*100) + "-intDuration-" + (int)(intDuration) + "-numOutreach-" + (int)(numOutreach) + "calib.txt";
		// String 	recorderName = "alcohol-apr" + "-intervention-" + (int)(intervention) + "-intTarget-" + (int)(intTarget) + "-intChange-" + (int)(intChange*100) + "-intDuration-" + (int)(intDuration) + "calib.txt";
		recorder = new DataRecorder(recorderName, this);

		// Number of agents
		recorder.addNumericDataSource("numAgents", new NumericDataSource() {
			public double execute() {
				int currAgents = agentList.size();
				return currAgents;
			}
		});
		
		// Socio-demographic characteristics of agents
		recorder.createAverageDataSource("meanage", agentList, "getAge");
		recorder.createAverageDataSource("pmale", agentList, "getGender");
		recorder.createAverageDataSource("page1", agentList, "getAge1");
		recorder.createAverageDataSource("page2", agentList, "getAge2");
		recorder.createAverageDataSource("page3", agentList, "getAge3");
		recorder.createAverageDataSource("page4", agentList, "getAge4");
		recorder.createAverageDataSource("page5", agentList, "getAge5");
		recorder.createAverageDataSource("page6", agentList, "getAge6");
		recorder.createAverageDataSource("pwhite", agentList, "getWhite");
		recorder.createAverageDataSource("pblack", agentList, "getBlack");
		recorder.createAverageDataSource("phisp", agentList, "getHisp");
		recorder.createAverageDataSource("pother", agentList, "getOtherRace");
		recorder.createAverageDataSource("plesshs", agentList, "getLesshs");
		recorder.createAverageDataSource("phs", agentList, "getHs");
		recorder.createAverageDataSource("pmorehs", agentList, "getMorehs");
		recorder.createAverageDataSource("pinc1", agentList, "getInc1");
		recorder.createAverageDataSource("pinc2", agentList, "getInc2");
		recorder.createAverageDataSource("pinc3", agentList, "getInc3");
		recorder.createAverageDataSource("pinc4", agentList, "getInc4");
		recorder.createAverageDataSource("pdurres1", agentList, "getDurRes1");
		recorder.createAverageDataSource("pdurres2", agentList, "getDurRes2");
		recorder.createAverageDataSource("pdurres3", agentList, "getDurRes3");
		recorder.createAverageDataSource("pmoved", agentList, "getMoved");
		
		// mortality
		recorder.createAverageDataSource("pdied", agentList, "getDied");
			// race-specific mortality
			recorder.createAverageDataSource("pwdied", wagentList, "getDied");
			recorder.createAverageDataSource("pbdied", bagentList, "getDied");
			recorder.createAverageDataSource("phdied", hagentList, "getDied");
			recorder.createAverageDataSource("podied", oagentList, "getDied");
			// gender-specific mortality
			recorder.createAverageDataSource("pmdied", magentList, "getDied");
			recorder.createAverageDataSource("pfdied", fagentList, "getDied");
			// age-specific mortality
			recorder.addNumericDataSource("page1died", new NumericDataSource() {
				public double execute() {
					int numage1 = 0;
					double avgage1died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge1()==1) {
							numage1 += 1;
							avgage1died += a.getDied();
						}
					}
					avgage1died /= (double) numage1;
					return avgage1died;
				}
			});
			recorder.addNumericDataSource("page2died", new NumericDataSource() {
				public double execute() {
					int numage2 = 0;
					double avgage2died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge2()==1) {
							numage2 += 1;
							avgage2died += a.getDied();
						}
					}
					avgage2died /= (double) numage2;
					return avgage2died;
				}
			});
			recorder.addNumericDataSource("page3died", new NumericDataSource() {
				public double execute() {
					int numage3 = 0;
					double avgage3died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge3()==1) {
							numage3 += 1;
							avgage3died += a.getDied();
						}
					}
					avgage3died /= (double) numage3;
					return avgage3died;
				}
			});
			recorder.addNumericDataSource("page4died", new NumericDataSource() {
				public double execute() {
					int numage4 = 0;
					double avgage4died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge4()==1) {
							numage4 += 1;
							avgage4died += a.getDied();
						}
					}
					avgage4died /= (double) numage4;
					return avgage4died;
				}
			});
			recorder.addNumericDataSource("page5died", new NumericDataSource() {
				public double execute() {
					int numage5 = 0;
					double avgage5died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge5()==1) {
							numage5 += 1;
							avgage5died += a.getDied();
						}
					}
					avgage5died /= (double) numage5;
					return avgage5died;
				}
			});
			recorder.addNumericDataSource("page6died", new NumericDataSource() {
				public double execute() {
					int numage6 = 0;
					double avgage6died = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						if (a.getAge6()==1) {
							numage6 += 1;
							avgage6died += a.getDied();
						}
					}
					avgage6died /= (double) numage6;
					return avgage6died;
				}
			});
					
	

					// drinking status -- non-drinker
					recorder.createAverageDataSource("pnondrk", agentList, "getNonDrinker");
						// race-specific non drinking status
						recorder.createAverageDataSource("pwnondrk", wagentList, "getNonDrinker");
						recorder.createAverageDataSource("pbnondrk", bagentList, "getNonDrinker");
						recorder.createAverageDataSource("phnondrk", hagentList, "getNonDrinker");
						recorder.createAverageDataSource("ponondrk", oagentList, "getNonDrinker");
					
					// drinking status -- light/moderate drinker
					recorder.createAverageDataSource("plight", agentList, "getLightDrinker");
						// race-specific light drinking status
						recorder.createAverageDataSource("pwlight", wagentList, "getLightDrinker");
						recorder.createAverageDataSource("pblight", bagentList, "getLightDrinker");
						recorder.createAverageDataSource("phlight", hagentList, "getLightDrinker");
						recorder.createAverageDataSource("polight", oagentList, "getLightDrinker");
						// gender-specific light drinking status
						recorder.createAverageDataSource("pmlight", magentList, "getLightDrinker");
						recorder.createAverageDataSource("pflight", fagentList, "getLightDrinker");
						// education-specific light drinking status
						recorder.createAverageDataSource("plesshslight", lesshsagentList, "getLightDrinker");
						recorder.createAverageDataSource("phslight",  hsagentList,  "getLightDrinker");
						recorder.createAverageDataSource("pmorehslight", morehsagentList, "getLightDrinker");
						// age-specific light drinking status
						recorder.addNumericDataSource("page1light", new NumericDataSource() {
							public double execute() {
								int numage1 = 0;
								double avgage1light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getAge1()==1) {
										numage1 += 1;
										avgage1light += a.getLightDrinker();
									}
								}
								avgage1light /= (double) numage1;
								return avgage1light;
							}
						});
						recorder.addNumericDataSource("page2light", new NumericDataSource() {
							public double execute() {
								int numage2 = 0;
								double avgage2light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getAge2()==1) {
										numage2 += 1;
										avgage2light += a.getLightDrinker();
									}
								}
								avgage2light /= (double) numage2;
								return avgage2light;
							}
						});
						recorder.addNumericDataSource("page3light", new NumericDataSource() {
							public double execute() {
								int numage3 = 0;
								double avgage3light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getAge3()==1) {
										numage3 += 1;
										avgage3light += a.getLightDrinker();
									}
								}
								avgage3light /= (double) numage3;
								return avgage3light;
							}
						});
						recorder.addNumericDataSource("page4light", new NumericDataSource() {
							public double execute() {
								int numage4 = 0;
								double avgage4light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getAge4()==1) {
										numage4 += 1;
										avgage4light += a.getLightDrinker();
									}
								}
								avgage4light /= (double) numage4;
								return avgage4light;
							}
						});
						recorder.addNumericDataSource("page5light", new NumericDataSource() {
							public double execute() {
								int numage5 = 0;
								double avgage5light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getAge5()==1) {
										numage5 += 1;
										avgage5light += a.getLightDrinker();
									}
								}
								avgage5light /= (double) numage5;
								return avgage5light;
							}
						});
						// income-specific light drinking status
						recorder.addNumericDataSource("pinc1light", new NumericDataSource() {
							public double execute() {
								int numinc1 = 0;
								double avginc1light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getInc1()==1) {
										numinc1 += 1;
										avginc1light += a.getLightDrinker();
									}
								}
								avginc1light /= (double) numinc1;
								return avginc1light;
							}
						});
						recorder.addNumericDataSource("pinc2light", new NumericDataSource() {
							public double execute() {
								int numinc2 = 0;
								double avginc2light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getInc2()==1) {
										numinc2 += 1;
										avginc2light += a.getLightDrinker();
									}
								}
								avginc2light /= (double) numinc2;
								return avginc2light;
							}
						});
						recorder.addNumericDataSource("pinc3light", new NumericDataSource() {
							public double execute() {
								int numinc3 = 0;
								double avginc3light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getInc3()==1) {
										numinc3 += 1;
										avginc3light += a.getLightDrinker();
									}
								}
								avginc3light /= (double) numinc3;
								return avginc3light;
							}
						});
						recorder.addNumericDataSource("pinc4light", new NumericDataSource() {
							public double execute() {
								int numinc4 = 0;
								double avginc4light = 0.0;
								for (int i=0; i<agentList.size(); i++) {
									AlcoholAgent a = (AlcoholAgent)agentList.get(i);
									if (a.getInc4()==1) {
										numinc4 += 1;
										avginc4light += a.getLightDrinker();
									}
								}
								avginc4light /= (double) numinc4;
								return avginc4light;
							}
						});
					
						// drinking status -- heavy drinker
						recorder.createAverageDataSource("pheavy", agentList, "getHeavyDrinker");
							// race-specific heavy drinking status
							recorder.createAverageDataSource("pwheavy", wagentList, "getHeavyDrinker");
							recorder.createAverageDataSource("pbheavy", bagentList, "getHeavyDrinker");
							recorder.createAverageDataSource("phheavy", hagentList, "getHeavyDrinker");
							recorder.createAverageDataSource("poheavy", oagentList, "getHeavyDrinker");
							// gender-specific heavy drinking status
							recorder.createAverageDataSource("pmheavy", magentList, "getHeavyDrinker");
							recorder.createAverageDataSource("pfheavy", fagentList, "getHeavyDrinker");
							// education-specific heavy drinking status
							recorder.createAverageDataSource("plesshsheavy", lesshsagentList, "getHeavyDrinker");
							recorder.createAverageDataSource("phsheavy",  hsagentList,  "getHeavyDrinker");
							recorder.createAverageDataSource("pmorehsheavy", morehsagentList, "getHeavyDrinker");
							// age-specific heavy drinking status
							recorder.addNumericDataSource("page1heavy", new NumericDataSource() {
								public double execute() {
									int numage1 = 0;
									double avgage1heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getAge1()==1) {
											numage1 += 1;
											avgage1heavy += a.getHeavyDrinker();
										}
									}
									avgage1heavy /= (double) numage1;
									return avgage1heavy;
								}
							});
							recorder.addNumericDataSource("page2heavy", new NumericDataSource() {
								public double execute() {
									int numage2 = 0;
									double avgage2heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getAge2()==1) {
											numage2 += 1;
											avgage2heavy += a.getHeavyDrinker();
										}
									}
									avgage2heavy /= (double) numage2;
									return avgage2heavy;
								}
							});
							recorder.addNumericDataSource("page3heavy", new NumericDataSource() {
								public double execute() {
									int numage3 = 0;
									double avgage3heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getAge3()==1) {
											numage3 += 1;
											avgage3heavy += a.getHeavyDrinker();
										}
									}
									avgage3heavy /= (double) numage3;
									return avgage3heavy;
								}
							});
							recorder.addNumericDataSource("page4heavy", new NumericDataSource() {
								public double execute() {
									int numage4 = 0;
									double avgage4heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getAge4()==1) {
											numage4 += 1;
											avgage4heavy += a.getHeavyDrinker();
										}
									}
									avgage4heavy /= (double) numage4;
									return avgage4heavy;
								}
							});
							recorder.addNumericDataSource("page5heavy", new NumericDataSource() {
								public double execute() {
									int numage5 = 0;
									double avgage5heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getAge5()==1) {
											numage5 += 1;
											avgage5heavy += a.getHeavyDrinker();
										}
									}
									avgage5heavy /= (double) numage5;
									return avgage5heavy;
								}
							});
							// income-specific heavy drinking status
							recorder.addNumericDataSource("pinc1heavy", new NumericDataSource() {
								public double execute() {
									int numinc1 = 0;
									double avginc1heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getInc1()==1) {
											numinc1 += 1;
											avginc1heavy += a.getHeavyDrinker();
										}
									}
									avginc1heavy /= (double) numinc1;
									return avginc1heavy;
								}
							});
							recorder.addNumericDataSource("pinc2heavy", new NumericDataSource() {
								public double execute() {
									int numinc2 = 0;
									double avginc2heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getInc2()==1) {
											numinc2 += 1;
											avginc2heavy += a.getHeavyDrinker();
										}
									}
									avginc2heavy /= (double) numinc2;
									return avginc2heavy;
								}
							});
							recorder.addNumericDataSource("pinc3heavy", new NumericDataSource() {
								public double execute() {
									int numinc3 = 0;
									double avginc3heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getInc3()==1) {
											numinc3 += 1;
											avginc3heavy += a.getHeavyDrinker();
										}
									}
									avginc3heavy /= (double) numinc3;
									return avginc3heavy;
								}
							});
							recorder.addNumericDataSource("pinc4heavy", new NumericDataSource() {
								public double execute() {
									int numinc4 = 0;
									double avginc4heavy = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getInc4()==1) {
											numinc4 += 1;
											avginc4heavy += a.getHeavyDrinker();
										}
									}
									avginc4heavy /= (double) numinc4;
									return avginc4heavy;
								}
							});
						
		// beverage type -- any drinking in past year
							recorder.addNumericDataSource("anybeer", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getDrinkStat()==2 || a.getDrinkStat()==3) {
											num += 1;
											avg += a.getAnyBeer();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("anywine", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getDrinkStat()==2 || a.getDrinkStat()==3) {
											num += 1;
											avg += a.getAnyWine();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("anyspirit", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if (a.getDrinkStat()==2 || a.getDrinkStat()==3) {
											num += 1;
											avg += a.getAnySpirit();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
												
							recorder.addNumericDataSource("pwanybeer", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getWhite()==1) {
											num += 1;
											avg += a.getAnyBeer();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("pwanywine", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getWhite()==1) {
											num += 1;
											avg += a.getAnyWine();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("pwanyspirit", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getWhite()==1) {
											num += 1;
											avg += a.getAnySpirit();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							
							recorder.addNumericDataSource("pbanybeer", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getBlack()==1) {
											num += 1;
											avg += a.getAnyBeer();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("pbanywine", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getBlack()==1) {
											num += 1;
											avg += a.getAnyWine();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							recorder.addNumericDataSource("pbanyspirit", new NumericDataSource() {
								public double execute() {
									int num = 0;
									double avg = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getBlack()==1) {
											num += 1;
											avg += a.getAnySpirit();
										}
									}
									avg /= (double) num;
									return avg;
								}
							});	
							
							recorder.addNumericDataSource("pinc1anybeer", new NumericDataSource() {
								public double execute() {
									int numinc1 = 0;
									double avginc1 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc1()==1) {
											numinc1 += 1;
											avginc1 += a.getAnyBeer();
										}
									}
									avginc1 /= (double) numinc1;
									return avginc1;
								}
							});
							recorder.addNumericDataSource("pinc2anybeer", new NumericDataSource() {
								public double execute() {
									int numinc2 = 0;
									double avginc2 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc2()==1) {
											numinc2 += 1;
											avginc2 += a.getAnyBeer();
										}
									}
									avginc2 /= (double) numinc2;
									return avginc2;
								}
							});
							recorder.addNumericDataSource("pinc3anybeer", new NumericDataSource() {
								public double execute() {
									int numinc3 = 0;
									double avginc3 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc3()==1) {
											numinc3 += 1;
											avginc3 += a.getAnyBeer();
										}
									}
									avginc3 /= (double) numinc3;
									return avginc3;
								}
							});
							recorder.addNumericDataSource("pinc4anybeer", new NumericDataSource() {
								public double execute() {
									int numinc4 = 0;
									double avginc4 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc4()==1) {
											numinc4 += 1;
											avginc4 += a.getAnyBeer();
										}
									}
									avginc4 /= (double) numinc4;
									return avginc4;
								}
							});
							
							recorder.addNumericDataSource("pinc1anywine", new NumericDataSource() {
								public double execute() {
									int numinc1 = 0;
									double avginc1 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc1()==1) {
											numinc1 += 1;
											avginc1 += a.getAnyWine();
										}
									}
									avginc1 /= (double) numinc1;
									return avginc1;
								}
							});
							recorder.addNumericDataSource("pinc2anywine", new NumericDataSource() {
								public double execute() {
									int numinc2 = 0;
									double avginc2 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc2()==1) {
											numinc2 += 1;
											avginc2 += a.getAnyWine();
										}
									}
									avginc2 /= (double) numinc2;
									return avginc2;
								}
							});
							recorder.addNumericDataSource("pinc3anywine", new NumericDataSource() {
								public double execute() {
									int numinc3 = 0;
									double avginc3 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc3()==1) {
											numinc3 += 1;
											avginc3 += a.getAnyWine();
										}
									}
									avginc3 /= (double) numinc3;
									return avginc3;
								}
							});
							recorder.addNumericDataSource("pinc4anywine", new NumericDataSource() {
								public double execute() {
									int numinc4 = 0;
									double avginc4 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc4()==1) {
											numinc4 += 1;
											avginc4 += a.getAnyWine();
										}
									}
									avginc4 /= (double) numinc4;
									return avginc4;
								}
							});
							
							recorder.addNumericDataSource("pinc1anyspirit", new NumericDataSource() {
								public double execute() {
									int numinc1 = 0;
									double avginc1 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc1()==1) {
											numinc1 += 1;
											avginc1 += a.getAnySpirit();
										}
									}
									avginc1 /= (double) numinc1;
									return avginc1;
								}
							});
							recorder.addNumericDataSource("pinc2anyspirit", new NumericDataSource() {
								public double execute() {
									int numinc2 = 0;
									double avginc2 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc2()==1) {
											numinc2 += 1;
											avginc2 += a.getAnySpirit();
										}
									}
									avginc2 /= (double) numinc2;
									return avginc2;
								}
							});
							recorder.addNumericDataSource("pinc3anyspirit", new NumericDataSource() {
								public double execute() {
									int numinc3 = 0;
									double avginc3 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc3()==1) {
											numinc3 += 1;
											avginc3 += a.getAnySpirit();
										}
									}
									avginc3 /= (double) numinc3;
									return avginc3;
								}
							});
							recorder.addNumericDataSource("pinc4anyspirit", new NumericDataSource() {
								public double execute() {
									int numinc4 = 0;
									double avginc4 = 0.0;
									for (int i=0; i<agentList.size(); i++) {
										AlcoholAgent a = (AlcoholAgent)agentList.get(i);
										if ((a.getDrinkStat()==2 || a.getDrinkStat()==3) && a.getInc4()==1) {
											numinc4 += 1;
											avginc4 += a.getAnySpirit();
										}
									}
									avginc4 /= (double) numinc4;
									return avginc4;
								}
							});
							
							
		// preferred beverage type
		recorder.addNumericDataSource("preferbeer", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getAnyBeer()==1) {
						num += 1;
						avg += a.getPreferBeer();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("preferwine", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getAnyWine()==1) {
						num += 1;
						avg += a.getPreferWine();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("preferspirit", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getAnySpirit()==1) {
						num += 1;
						avg += a.getPreferSpirit();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
							
		recorder.addNumericDataSource("pwbeer", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getWhite()==1) {
						num += 1;
						avg += a.getPreferBeer();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("pwwine", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getWhite()==1) {
						num += 1;
						avg += a.getPreferWine();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("pwspirit", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getWhite()==1) {
						num += 1;
						avg += a.getPreferSpirit();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		
		recorder.addNumericDataSource("pbbeer", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getBlack()==1) {
						num += 1;
						avg += a.getPreferBeer();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("pbwine", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getBlack()==1) {
						num += 1;
						avg += a.getPreferWine();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		recorder.addNumericDataSource("pbspirit", new NumericDataSource() {
			public double execute() {
				int num = 0;
				double avg = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getBlack()==1) {
						num += 1;
						avg += a.getPreferSpirit();
					}
				}
				avg /= (double) num;
				return avg;
			}
		});	
		
		recorder.addNumericDataSource("pinc1beer", new NumericDataSource() {
			public double execute() {
				int numinc1 = 0;
				double avginc1 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getInc1()==1) {
						numinc1 += 1;
						avginc1 += a.getPreferBeer();
					}
				}
				avginc1 /= (double) numinc1;
				return avginc1;
			}
		});
		recorder.addNumericDataSource("pinc2beer", new NumericDataSource() {
			public double execute() {
				int numinc2 = 0;
				double avginc2 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getInc2()==1) {
						numinc2 += 1;
						avginc2 += a.getPreferBeer();
					}
				}
				avginc2 /= (double) numinc2;
				return avginc2;
			}
		});
		recorder.addNumericDataSource("pinc3beer", new NumericDataSource() {
			public double execute() {
				int numinc3 = 0;
				double avginc3 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getInc3()==1) {
						numinc3 += 1;
						avginc3 += a.getPreferBeer();
					}
				}
				avginc3 /= (double) numinc3;
				return avginc3;
			}
		});
		recorder.addNumericDataSource("pinc4beer", new NumericDataSource() {
			public double execute() {
				int numinc4 = 0;
				double avginc4 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyBeer()==1) && a.getInc4()==1) {
						numinc4 += 1;
						avginc4 += a.getPreferBeer();
					}
				}
				avginc4 /= (double) numinc4;
				return avginc4;
			}
		});
		
		recorder.addNumericDataSource("pinc1wine", new NumericDataSource() {
			public double execute() {
				int numinc1 = 0;
				double avginc1 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getInc1()==1) {
						numinc1 += 1;
						avginc1 += a.getPreferWine();
					}
				}
				avginc1 /= (double) numinc1;
				return avginc1;
			}
		});
		recorder.addNumericDataSource("pinc2wine", new NumericDataSource() {
			public double execute() {
				int numinc2 = 0;
				double avginc2 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getInc2()==1) {
						numinc2 += 1;
						avginc2 += a.getPreferWine();
					}
				}
				avginc2 /= (double) numinc2;
				return avginc2;
			}
		});
		recorder.addNumericDataSource("pinc3wine", new NumericDataSource() {
			public double execute() {
				int numinc3 = 0;
				double avginc3 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getInc3()==1) {
						numinc3 += 1;
						avginc3 += a.getPreferWine();
					}
				}
				avginc3 /= (double) numinc3;
				return avginc3;
			}
		});
		recorder.addNumericDataSource("pinc4wine", new NumericDataSource() {
			public double execute() {
				int numinc4 = 0;
				double avginc4 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnyWine()==1) && a.getInc4()==1) {
						numinc4 += 1;
						avginc4 += a.getPreferWine();
					}
				}
				avginc4 /= (double) numinc4;
				return avginc4;
			}
		});
		
		recorder.addNumericDataSource("pinc1spirit", new NumericDataSource() {
			public double execute() {
				int numinc1 = 0;
				double avginc1 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getInc1()==1) {
						numinc1 += 1;
						avginc1 += a.getPreferSpirit();
					}
				}
				avginc1 /= (double) numinc1;
				return avginc1;
			}
		});
		recorder.addNumericDataSource("pinc2spirit", new NumericDataSource() {
			public double execute() {
				int numinc2 = 0;
				double avginc2 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getInc2()==1) {
						numinc2 += 1;
						avginc2 += a.getPreferSpirit();
					}
				}
				avginc2 /= (double) numinc2;
				return avginc2;
			}
		});
		recorder.addNumericDataSource("pinc3spirit", new NumericDataSource() {
			public double execute() {
				int numinc3 = 0;
				double avginc3 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getInc3()==1) {
						numinc3 += 1;
						avginc3 += a.getPreferSpirit();
					}
				}
				avginc3 /= (double) numinc3;
				return avginc3;
			}
		});
		recorder.addNumericDataSource("pinc4spirit", new NumericDataSource() {
			public double execute() {
				int numinc4 = 0;
				double avginc4 = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if ((a.getAnySpirit()==1) && a.getInc4()==1) {
						numinc4 += 1;
						avginc4 += a.getPreferSpirit();
					}
				}
				avginc4 /= (double) numinc4;
				return avginc4;
			}
		});
							
		///////////////////////// drinking status transitions
		// among baseline non-drinkers
		recorder.createAverageDataSource("pnondrk1", baseNonDrkList, "getNonDrinker");
		recorder.createAverageDataSource("pnondrk2", baseNonDrkList, "getLightDrinker");
		recorder.createAverageDataSource("pnondrk3", baseNonDrkList, "getHeavyDrinker");
		// among baseline light/moderate drinkers
		recorder.createAverageDataSource("plightdrk1", baseLightDrkList, "getNonDrinker");
		recorder.createAverageDataSource("plightdrk2", baseLightDrkList, "getLightDrinker");
		recorder.createAverageDataSource("plightdrk3", baseLightDrkList, "getHeavyDrinker");
		// among baseline heavy drinkers
		recorder.createAverageDataSource("pheavydrk1", baseHeavyDrkList, "getNonDrinker");
		recorder.createAverageDataSource("pheavydrk2", baseHeavyDrkList, "getLightDrinker");
		recorder.createAverageDataSource("pheavydrk3", baseHeavyDrkList, "getHeavyDrinker");
		
		///////////////// annual transitions 
		
		// non-drinkers who become light drinkers
		recorder.addNumericDataSource("pnon2light", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPLight = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPLight += a.getLightDrinker();
					}
				}
				avgPLight /= (double) numNonDrk;
				return avgPLight;
			}
		});
		
		// non-drinkers who remain non-drinkers
		recorder.addNumericDataSource("pnon2non", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numNonDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who become non-drinkers
		recorder.addNumericDataSource("plight2non", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numLightDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who remained light drinkers
		recorder.addNumericDataSource("plight2light", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numLightDrk;
				return avgPLightDrk;
			}
		});
		
		// light drinkers who became heavy drinkers
		recorder.addNumericDataSource("plight2heavy", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numLightDrk;
				return avgPHeavyDrk;
			}
		});
		
		// heavy drinkers who became light drinkers
		recorder.addNumericDataSource("pheavy2light", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numHeavyDrk;
				return avgPLightDrk;
			}
		});
		
		// heavy drinkers who remained heavy drinkers
		recorder.addNumericDataSource("pheavy2heavy", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numHeavyDrk;
				return avgPHeavyDrk;
			}
		});
		
		/////////////// DRINKING TRANSITIONS AMONG WHITES
		// non-drinkers who become light drinkers
		recorder.addNumericDataSource("wpnon2light", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPLight = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPLight += a.getLightDrinker();
					}
				}
				avgPLight /= (double) numNonDrk;
				return avgPLight;
			}
		});
		
		// non-drinkers who remain non-drinkers
		recorder.addNumericDataSource("wpnon2non", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numNonDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who become non-drinkers
		recorder.addNumericDataSource("wplight2non", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numLightDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who remained light drinkers
		recorder.addNumericDataSource("wplight2light", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numLightDrk;
				return avgPLightDrk;
			}
		});
		
		// light drinkers who became heavy drinkers
		recorder.addNumericDataSource("wplight2heavy", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numLightDrk;
				return avgPHeavyDrk;
			}
		});
		
		// heavy drinkers who became light drinkers
		recorder.addNumericDataSource("wpheavy2light", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numHeavyDrk;
				return avgPLightDrk;
			}
		});
		
		// heavy drinkers who remained heavy drinkers
		recorder.addNumericDataSource("wpheavy2heavy", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<wagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)wagentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numHeavyDrk;
				return avgPHeavyDrk;
			}
		});
		
		/////////////// DRINKING TRANSITIONS AMONG BLACKS
		// non-drinkers who become light drinkers
		recorder.addNumericDataSource("bpnon2light", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPLight = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPLight += a.getLightDrinker();
					}
				}
				avgPLight /= (double) numNonDrk;
				return avgPLight;
			}
		});
		
		// non-drinkers who remain non-drinkers
		recorder.addNumericDataSource("bpnon2non", new NumericDataSource() {
			public double execute() {
				int numNonDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==1 && a.getDoNotCount()==0) {
						numNonDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numNonDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who become non-drinkers
		recorder.addNumericDataSource("bplight2non", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPNonDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPNonDrk += a.getNonDrinker();
					}
				}
				avgPNonDrk /= (double) numLightDrk;
				return avgPNonDrk;
			}
		});
		
		// light drinkers who remained light drinkers
		recorder.addNumericDataSource("bplight2light", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numLightDrk;
				return avgPLightDrk;
			}
		});
		
		// light drinkers who became heavy drinkers
		recorder.addNumericDataSource("bplight2heavy", new NumericDataSource() {
			public double execute() {
				int numLightDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==2 && a.getDoNotCount()==0) {
						numLightDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numLightDrk;
				return avgPHeavyDrk;
			}
		});
		
		// heavy drinkers who became light drinkers
		recorder.addNumericDataSource("bpheavy2light", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPLightDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPLightDrk += a.getLightDrinker();
					}
				}
				avgPLightDrk /= (double) numHeavyDrk;
				return avgPLightDrk;
			}
		});
		
		// heavy drinkers who remained heavy drinkers
		recorder.addNumericDataSource("bpheavy2heavy", new NumericDataSource() {
			public double execute() {
				int numHeavyDrk = 0;
				double avgPHeavyDrk = 0.0;
				for (int i=0; i<bagentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)bagentList.get(i);
					if (a.getLastDrinkStat()==3 && a.getDoNotCount()==0) {
						numHeavyDrk += 1;
						avgPHeavyDrk += a.getHeavyDrinker();
					}
				}
				avgPHeavyDrk /= (double) numHeavyDrk;
				return avgPHeavyDrk;
			}
		});
		
		
		// Violence and trauma among agents
		recorder.createAverageDataSource("ppotviolvict", agentList, "getPotviolvict");
		recorder.createAverageDataSource("pviolvict", agentList, "getViolvict");
		recorder.createAverageDataSource("palcviol", agentList, "getAlcViol");
		recorder.createAverageDataSource("peverviolvict", agentList, "getPriorviolvict");
		recorder.createAverageDataSource("ppotviolperp", agentList, "getPotviolperp");
		recorder.createAverageDataSource("pviolperp", agentList, "getViolperp");
		recorder.createAverageDataSource("peverviolperp", agentList, "getPriorviolperp");
		recorder.createAverageDataSource("phom", agentList, "getHomicide");
		recorder.createAverageDataSource("palchom", agentList, "getAlcHomicide");
		
		
			// violent victimization in neighborhoods targeted in intervention
			recorder.addNumericDataSource("pvioltarget", new NumericDataSource() {
				public double execute() {
					int targetpop = 0;
					double avgviol = 0.0;
					for (int i=0; i<agentList.size(); i++) {
						AlcoholAgent a = (AlcoholAgent)agentList.get(i);
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(a.Agenthood);
						if (nb.getTargetHood() == 1) {
							targetpop += 1;
							avgviol += a.getViolvict();
						}
					}
					avgviol /= (double) targetpop;
					return avgviol;
				}
			});
			
			// average number of violent events in each neighborhood targeted in intervention
			recorder.addNumericDataSource("numvioltarget",  new NumericDataSource() {
				public double execute() {
					int numhood = 0;
					double avgnum = 0.0;
					for (int j=0; j<hoodList.size(); j++) {
						AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(j);
						if (nb.getTargetHood() == 1) {
							numhood += 1;
							avgnum += nb.getNumviolevent();
						}
					}
					avgnum /= (double) numhood;
					return avgnum;
				}
			});
		
		// movement from high- to low-income neighborhoods
		recorder.addNumericDataSource("phigh2low", new NumericDataSource() {
			public double execute() {
				int numhighhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getBaseIncHood()==1) {
						numhighhood += 1;
						avgmove += a.getEverLowInc();
					}
				}
				avgmove /= (double) numhighhood;
				return avgmove;
			}
		});
		
		// WHITES
		recorder.addNumericDataSource("wphigh2low", new NumericDataSource() {
			public double execute() {
				int numhighhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getRace()==1 && a.getBaseIncHood()==1) {
						numhighhood += 1;
						avgmove += a.getEverLowInc();
					}
				}
				avgmove /= (double) numhighhood;
				return avgmove;
			}
		});
		
		// BLACKS
		recorder.addNumericDataSource("bphigh2low", new NumericDataSource() {
			public double execute() {
				int numhighhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getRace()==2 && a.getBaseIncHood()==1) {
						numhighhood += 1;
						avgmove += a.getEverLowInc();
					}
				}
				avgmove /= (double) numhighhood;
				return avgmove;
			}
		});
		
		
		// movement from low- to high-income neighborhoods
		recorder.addNumericDataSource("plow2high", new NumericDataSource() {
			public double execute() {
				int numlowhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getBaseIncHood()==2) {
						numlowhood += 1;
						avgmove += a.getEverHighInc();
					}
				}
				avgmove /= (double) numlowhood;
				return avgmove;
			}
		});
		
		// WHITES
		recorder.addNumericDataSource("wplow2high", new NumericDataSource() {
			public double execute() {
				int numlowhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getRace()==1 && a.getBaseIncHood()==2) {
						numlowhood += 1;
						avgmove += a.getEverHighInc();
					}
				}
				avgmove /= (double) numlowhood;
				return avgmove;
			}
		});
		
		// BLACKS
		recorder.addNumericDataSource("bplow2high", new NumericDataSource() {
			public double execute() {
				int numlowhood = 0;
				double avgmove = 0.0;
				for (int i=0; i<agentList.size(); i++) {
					AlcoholAgent a = (AlcoholAgent)agentList.get(i);
					if (a.getRace()==2 && a.getBaseIncHood()==2) {
						numlowhood += 1;
						avgmove += a.getEverHighInc();
					}
				}
				avgmove /= (double) numlowhood;
				return avgmove;
			}
		});
		
		
		// Racial disparities in violence and outcomes
		
		// Outcomes among WHITES
		recorder.createAverageDataSource("wviolvict", wagentList, "getViolvict");
		recorder.createAverageDataSource("walcviol", wagentList, "getAlcViol");
		recorder.createAverageDataSource("weverviolvict", wagentList, "getPriorviolvict");
		recorder.createAverageDataSource("wviolperp", wagentList, "getViolperp");
		recorder.createAverageDataSource("weverviolperp", wagentList, "getPriorviolperp");
		recorder.createAverageDataSource("whom", wagentList, "getHomicide");
		recorder.createAverageDataSource("walchom", wagentList, "getAlcHomicide");
		
		// Outcomes among BLACKS
		recorder.createAverageDataSource("bviolvict", bagentList, "getViolvict");
		recorder.createAverageDataSource("balcviol", bagentList, "getAlcViol");
		recorder.createAverageDataSource("beverviolvict", bagentList, "getPriorviolvict");
		recorder.createAverageDataSource("bviolperp", bagentList, "getViolperp");
		recorder.createAverageDataSource("beverviolperp", bagentList, "getPriorviolperp");
		recorder.createAverageDataSource("bhom", bagentList, "getHomicide");
		recorder.createAverageDataSource("balchom", bagentList, "getAlcHomicide");
		
		// Neighborhood characteristics
		recorder.createAverageDataSource("hoodviol", hoodList, "getAvghoodviol");
		recorder.createAverageDataSource("avghoodinc", hoodList, "getAvghoodinc");
		recorder.createAverageDataSource("hoodinc1", hoodList, "getHoodinc1");
		recorder.createAverageDataSource("hoodinc2", hoodList, "getHoodinc2");
		recorder.createAverageDataSource("hoodblack", hoodList, "getPercBlack");
		recorder.createAverageDataSource("hoodhisp", hoodList, "getPercHisp");
		recorder.createAverageDataSource("police", hoodList, "getNumPolice");


		} // end of recordOutput()
		
		// Record output for each neighborhood just to make sure that neighborhood composition matches expected distributions
		public void recordHoodOutput() {

			recorder = new DataRecorder("alcohol_checkhoods_revised.txt", this);
			
			// number of agents and socio-demographic characteristics of neighborhoods
			for (int t=0; t<hoodList.size(); t++) {
				AlcoholNeighborhood nb = (AlcoholNeighborhood)hoodList.get(t);
				String s;
				s = String.format( "_h%s", t );
				
				// number of agents
				recorder.createNumericDataSource("numAgents"+s, nb, "getHoodSize");
				
				// socio-demographic characteristics
				recorder.createAverageDataSource("pmale"+s, nb.neighborhoodAgentList, "getGender");
				recorder.createAverageDataSource("page1"+s, nb.neighborhoodAgentList, "getAge1");
				recorder.createAverageDataSource("page2"+s, nb.neighborhoodAgentList, "getAge2");
				recorder.createAverageDataSource("page3"+s, nb.neighborhoodAgentList, "getAge3");
				recorder.createAverageDataSource("page4"+s, nb.neighborhoodAgentList, "getAge4");
				recorder.createAverageDataSource("page5"+s, nb.neighborhoodAgentList, "getAge5");
				recorder.createAverageDataSource("pwhite"+s, nb.neighborhoodAgentList, "getWhite");
				recorder.createAverageDataSource("pblack"+s, nb.neighborhoodAgentList, "getBlack");
				recorder.createAverageDataSource("phisp"+s, nb.neighborhoodAgentList, "getHisp");
				recorder.createAverageDataSource("pother"+s, nb.neighborhoodAgentList, "getOtherRace");
				recorder.createAverageDataSource("pinc1"+s, nb.neighborhoodAgentList, "getInc1");
				recorder.createAverageDataSource("pinc2"+s, nb.neighborhoodAgentList, "getInc2");
				recorder.createAverageDataSource("pinc3"+s, nb.neighborhoodAgentList, "getInc3");
				recorder.createAverageDataSource("pinc4"+s, nb.neighborhoodAgentList, "getInc4");
				recorder.createNumericDataSource("income"+s, nb, "getAvghoodinc");

				// violent victimization
				// recorder.createAverageDataSource("pviolvict"+s, nb.neighborhoodAgentList, "getViolvict");
				
				// alcohol outlet density
				// recorder.createNumericDataSource("alcdens"+s, nb, "getAlcDensity");
				
				// community policing
				// recorder.createNumericDataSource("policing"+s, nb, "getPolicing");
				
				// drug markets
				// recorder.createNumericDataSource("drugMarket"+s, nb, "getDrugMarket");
				
				// drinking norms
				// recorder.createNumericDataSource("drinknorm"+s, nb, "getDrinknorm");
				
				// average percent light and heavy drinkers at neighborhood outlets
				// recorder.createAverageDataSource("plightdrk"+s, nb.neighborhoodOutletList, "getPLightDrk");
				// recorder.createAverageDataSource("pheavydrk"+s, nb.neighborhoodOutletList, "getPHeavyDrk");
			}
			
		} // end of recordHoodOutput()
				
	} // end of AlcoholModel class

	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	//auxilliary class for file opening/closing
	//and string processing
	//


	class IOUtils {

		public static String readBRLine ( BufferedReader file ) {
			String s;
			try {
				s = file.readLine();
				} catch  ( IOException e ) {
				//System.out.println( "closeBRFile error!" );
				s = null;
			}
			return s;
		}

		public  static BufferedReader openFileToRead ( String filename ) {
			BufferedReader in;
			try {
				in = new BufferedReader( new FileReader(filename));
				} catch ( IOException e ) {
				// no file, etc
				// System.out.println( "openFileToRead error on filename="+filename );
				in = null;
			}
			// System.err.printf("openFileToRead: '%s'\n", filename );
			return in;
		}

		public static PrintWriter openFileToWrite ( String dir, String filename, String how ) {
			PrintWriter out;
			try {
				File f = new File( dir, filename );
				out = new PrintWriter( new FileWriter(f) );
				} catch ( IOException e ) {
				// no file, etc
				//System.out.println( "openFileToWrite error on dir/filename="
				//					+ dir + "/" + filename );
				out = null;
				}
			//System.err.printf("openFileToWrite: '%s'\n", filename );
			return out;
		}

		public  static int closeBRFile (  BufferedReader file ) {
			int r = 0;
			try {
				file.close();
				} catch  ( IOException e ) {
				//System.out.println( "closeBRFile error!" );
				r = -1;
			}
			return r;
		}

		public static int tokenToInt( String token ) {
			int i;
			token = token.trim();
			try {
				i = Integer.parseInt( token );
				} catch (NumberFormatException ex) {
				throw new IllegalArgumentException(" tokenToInt error, token="+token ); 
			}
			return i;
		}

		public static double tokenToDouble( String token ) {
			double d;
			token = token.trim();
			try {
				d = Double.parseDouble( token );
				} catch (NumberFormatException ex) {
				throw new IllegalArgumentException(" tokenToDouble error, token="+token ); 
			}
			return d;
		}

	} // end of IOUtils class