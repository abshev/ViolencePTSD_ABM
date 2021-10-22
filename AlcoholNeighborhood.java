/*
 * Alcohol ABM
 *
 *     This model will compare interventions aimed at reducing racial disparities in alcohol-related
 * homicide, using New York City as the place and population of interest.
 *
 *     The neighborhood class creates the neighborhoods in which the agents reside. These neighborhoods will
 * have characteristics that are aggregates of their agent residents, as well as characteristics set
 * externally. Neighborhood characteristics influence agent exposure to violence and substance use.
 * 
 * Revised Oct 20, 2014
 * 
 * Revisions:	(1) Changed drinking norm to unacceptability of drunkenness
 * 				(2) Added proportion of cells with on- and off-premise alcohol outlets (derived from density and number of each type of outlet)
 *  			(3) When cells within neighborhoods are created, locations of alcohol outlets are randomly assigned
 *  			(4) Neighborhood drinking norm is aggregated from individual negative attitudes towards frequent drunkenness
 *  			(5) Modified number of on- and off-premise outlets to reflect increased size of the model world (5% instead of 1%)
 *  			(6) Removed neighborhood norms towards drinking
 */

package cbtModel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.Named;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.space.Object2DTorus;
import uchicago.src.sim.util.Random;

import cern.jet.math.*;

@SuppressWarnings("unused")
public class AlcoholNeighborhood {

	// variable declarations - attributes of each neighborhood and other 

		// relevant variables
		public static int		nextID = 0;		// to give each neighborhood an ID
		int						ID;
		private double			num_nb;			// number of neighborhoods in model
		public int				nb_minX;		// neighborhood boundaries
		public int				nb_maxX;		// neighborhood boundaries
		public int				nb_minY;		// neighborhood boundaries
		public int				nb_maxY;		// neighborhood boundaries
		private Object2DGrid 	hoodSpace;
		public int				cdcode;			// actual CD ID number for each neighborhood
		public int				hoodSize;		// total agent population in each neighborhood

		// characteristics of neighborhoods
		
		// neighborhood income
		public double	avghoodinc;			// average neighborhood income
		public int		hoodinc;			// neighborhood income categories 1 <$44k, 2 $44-57k, 3 $58k+
		public int		highhoodinc;		// above average neighborhood income
		public double 	lastavghoodinc;		// average neighborhood income at last time step
		public double	changeincome; 		// change in average neighborhood income between time steps
		
		// neighborhood racial/ethnic composition
		public double	percBlack;			// percent black residents in neighborhood
		public double	percHisp;			// percent Hispanic residents in neighborhood
		
		// neighborhood mean age
		public double	avgAge;				// mean age of neighborhood residents
		public double	percYoungMale;		// percent of population young men aged 18-24 yrs old
		
		// neighborhood violence
		public double	avghoodviol;		// average level of neighborhood violence at current time step
		public double	lastavghoodviol;	// average level of neighborhood violence at last time step
		public double 	avghoodperp;		// average level of neighborhood perpetration at current time step
		public int		highhoodviol;		// high (above average) level of neighborhood violence
		public double	changeviol;			// change in average neighborhood violence between time steps
		public int		numviolevent;		// number of violent events at current time step
		public double	homrate;			// homicide rate per 100,000 population
		public double	alchomrate;			// alcohol-related homicide rate per 100,000 population
		
		// neighborhood residential stability
		public double	percStable;			// percent who lived in neighborhood for at least 1 year
		
		// neighborhood composition
		public double	percFBorn;			// percent foreign-born
		public double 	percManProf;		// percent in professional/managerial occupations
		public double	percUnemp;			// percent unemployed
		public double	percFemHHKids;		// percent female-headed households with kids < 18 yrs old
		
		// neighborhood drinking
		public double	percLightDrk;		// percent light/moderate drinkers
		public double 	percHeavyDrk;		// percent heavy drinkers
		
		// dummy variables for neighborhood characteristics
		public int		hoodinc1, hoodinc2;	// $58k+ (hoodinc=3) is referent
		public int		targetHood;			// neighborhood targeted for policing intervention

		// list of agents in each neighborhood
		public ArrayList<AlcoholAgent>         neighborhoodAgentList=new ArrayList<AlcoholAgent>();
		public ArrayList<AlcoholAgent>		   temphoodAgentList=new ArrayList<AlcoholAgent>();
		
		// list of cells in each neighborhood
		public ArrayList<AlcoholCell>		  	neighborhoodCellList = new ArrayList<AlcoholCell>();
		

		
		
		// the Neighborhood constructor
		public AlcoholNeighborhood (int nbID, Object2DGrid cellSpace, double percBars) {
			ID = nbID;	// ID starts from 0
			hoodSpace = cellSpace;

			// Array to hold list of agents in neighborhood
			neighborhoodAgentList=new ArrayList<AlcoholAgent>();
			temphoodAgentList=new ArrayList<AlcoholAgent>();
			

			// Assign neighborhood boundaries
			// NOTE: Grid of 59 neighborhoods is an abstract representation of NYC CDs in size and approximate location
			if (ID ==	0	) 	  {nb_minX =	60	; nb_maxX =	140	; nb_minY =	78	; nb_maxY =	98	; }
			else if (ID ==	1	) {nb_minX =	140	; nb_maxX =	230	; nb_minY =	78	; nb_maxY =	98	; }
			else if (ID ==	2	) {nb_minX =	160	; nb_maxX =	230	; nb_minY =	60	; nb_maxY =	78	; }
			else if (ID ==	3	) {nb_minX =	60	; nb_maxX =	160	; nb_minY =	60	; nb_maxY =	78	; }
			else if (ID ==	4	) {nb_minX =	60	; nb_maxX =	120	; nb_minY =	40	; nb_maxY =	60	; }
			else if (ID ==	5	) {nb_minX =	120	; nb_maxX =	230	; nb_minY =	40	; nb_maxY =	60	; }
			else if (ID ==	6	) {nb_minX =	60	; nb_maxX =	230	; nb_minY =	18	; nb_maxY =	40	; }
			else if (ID ==	7	) {nb_minX =	0	; nb_maxX =	60	; nb_minY =	0	; nb_maxY =	42	; }
			else if (ID ==	8	) {nb_minX =	230	; nb_maxX =	288	; nb_minY =	36	; nb_maxY =	88	; }
			else if (ID ==	9	) {nb_minX =	288	; nb_maxX =	400	; nb_minY =	36	; nb_maxY =	88	; }
			else if (ID ==	10	) {nb_minX =	230	; nb_maxX =	400	; nb_minY =	18	; nb_maxY =	36	; }
			else if (ID ==	11	) {nb_minX =	60	; nb_maxX =	400	; nb_minY =	0	; nb_maxY =	18	; }
			else if (ID ==	12	) {nb_minX =	60	; nb_maxX =	205	; nb_minY =	182	; nb_maxY =	210	; }
			else if (ID ==	13	) {nb_minX =	60	; nb_maxX =	160	; nb_minY =	210	; nb_maxY =	230	; }
			else if (ID ==	14	) {nb_minX =	160	; nb_maxX =	280	; nb_minY =	210	; nb_maxY =	230	; }
			else if (ID ==	15	) {nb_minX =	205	; nb_maxX =	264	; nb_minY =	182	; nb_maxY =	210	; }
			else if (ID ==	16	) {nb_minX =	182	; nb_maxX =	280	; nb_minY =	230	; nb_maxY =	286	; }
			else if (ID ==	17	) {nb_minX =	60	; nb_maxX =	106	; nb_minY =	230	; nb_maxY =	286	; }
			else if (ID ==	18	) {nb_minX =	60	; nb_maxX =	150	; nb_minY =	286	; nb_maxY =	318	; }
			else if (ID ==	19	) {nb_minX =	106	; nb_maxX =	154	; nb_minY =	230	; nb_maxY =	258	; }
			else if (ID ==	20	) {nb_minX =	106	; nb_maxX =	154	; nb_minY =	258	; nb_maxY =	286	; }
			else if (ID ==	21	) {nb_minX =	60	; nb_maxX =	150	; nb_minY =	318	; nb_maxY =	350	; }
			else if (ID ==	22	) {nb_minX =	180	; nb_maxX =	280	; nb_minY =	350	; nb_maxY =	380	; }
			else if (ID ==	23	) {nb_minX =	150	; nb_maxX =	235	; nb_minY =	286	; nb_maxY =	318	; }
			else if (ID ==	24	) {nb_minX =	180	; nb_maxX =	215	; nb_minY =	380	; nb_maxY =	455	; }
			else if (ID ==	25	) {nb_minX =	150	; nb_maxX =	235	; nb_minY =	318	; nb_maxY =	350	; }
			else if (ID ==	26	) {nb_minX =	215	; nb_maxX =	280	; nb_minY =	380	; nb_maxY =	455	; }
			else if (ID ==	27	) {nb_minX =	154	; nb_maxX =	182	; nb_minY =	230	; nb_maxY =	286	; }
			else if (ID ==	28	) {nb_minX =	235	; nb_maxX =	280	; nb_minY =	286	; nb_maxY =	350	; }
			else if (ID ==	29	) {nb_minX =	180	; nb_maxX =	280	; nb_minY =	455	; nb_maxY =	555	; }
			else if (ID ==	30	) {nb_minX =	0	; nb_maxX =	60	; nb_minY =	328	; nb_maxY =	350	; }
			else if (ID ==	31	) {nb_minX =	0	; nb_maxX =	30	; nb_minY =	282	; nb_maxY =	328	; }
			else if (ID ==	32	) {nb_minX =	30	; nb_maxX =	60	; nb_minY =	282	; nb_maxY =	328	; }
			else if (ID ==	33	) {nb_minX =	0	; nb_maxX =	21	; nb_minY =	222	; nb_maxY =	282	; }
			else if (ID ==	34	) {nb_minX =	21	; nb_maxX =	40	; nb_minY =	222	; nb_maxY =	282	; }
			else if (ID ==	35	) {nb_minX =	40	; nb_maxX =	60	; nb_minY =	222	; nb_maxY =	282	; }
			else if (ID ==	36	) {nb_minX =	0	; nb_maxX =	30	; nb_minY =	164	; nb_maxY =	222	; }
			else if (ID ==	37	) {nb_minX =	30	; nb_maxX =	60	; nb_minY =	164	; nb_maxY =	222	; }
			else if (ID ==	38	) {nb_minX =	0	; nb_maxX =	12	; nb_minY =	80	; nb_maxY =	164	; }
			else if (ID ==	39	) {nb_minX =	12	; nb_maxX =	36	; nb_minY =	80	; nb_maxY =	164	; }
			else if (ID ==	40	) {nb_minX =	36	; nb_maxX =	60	; nb_minY =	80	; nb_maxY =	164	; }
			else if (ID ==	41	) {nb_minX =	0	; nb_maxX =	60	; nb_minY =	42	; nb_maxY =	80	; }
			else if (ID ==	42	) {nb_minX =	60	; nb_maxX =	230	; nb_minY =	98	; nb_maxY =	130	; }
			else if (ID ==	43	) {nb_minX =	60	; nb_maxX =	264	; nb_minY =	130	; nb_maxY =	152	; }
			else if (ID ==	44	) {nb_minX =	230	; nb_maxX =	324	; nb_minY =	88	; nb_maxY =	130	; }
			else if (ID ==	45	) {nb_minX =	264	; nb_maxX =	324	; nb_minY =	130	; nb_maxY =	162	; }
			else if (ID ==	46	) {nb_minX =	60	; nb_maxX =	264	; nb_minY =	152	; nb_maxY =	182	; }
			else if (ID ==	47	) {nb_minX =	264	; nb_maxX =	324	; nb_minY =	162	; nb_maxY =	210	; }
			else if (ID ==	48	) {nb_minX =	324	; nb_maxX =	400	; nb_minY =	88	; nb_maxY =	210	; }
			else if (ID ==	49	) {nb_minX =	306	; nb_maxX =	348	; nb_minY =	210	; nb_maxY =	360	; }
			else if (ID ==	50	) {nb_minX =	280	; nb_maxX =	306	; nb_minY =	210	; nb_maxY =	360	; }
			else if (ID ==	51	) {nb_minX =	280	; nb_maxX =	307	; nb_minY =	360	; nb_maxY =	555	; }
			else if (ID ==	52	) {nb_minX =	348	; nb_maxX =	400	; nb_minY =	210	; nb_maxY =	360	; }
			else if (ID ==	53	) {nb_minX =	307	; nb_maxX =	347	; nb_minY =	360	; nb_maxY =	555	; }
			else if (ID ==	54	) {nb_minX =	347	; nb_maxX =	400	; nb_minY =	360	; nb_maxY =	555	; }
			else if (ID ==	55	) {nb_minX =	180	; nb_maxX =	400	; nb_minY =	555	; nb_maxY =	625	; }
			else if (ID ==	56	) {nb_minX =	0	; nb_maxX =	180	; nb_minY =	350	; nb_maxY =	415	; }
			else if (ID ==	57	) {nb_minX =	0	; nb_maxX =	180	; nb_minY =	415	; nb_maxY =	520	; }
			else if (ID ==	58	) {nb_minX =	0	; nb_maxX =	180	; nb_minY =	520	; nb_maxY =	625	; }
			
			// Assign UHF id to each neighborhood
			if (ID >=0 && ID <= 11) {cdcode = ID + 101;}
			else if (ID >= 12 && ID <= 29) {cdcode = (ID + 200) - 11;}
			else if (ID >= 30 && ID <= 41) {cdcode = (ID + 300) - 29;}
			else if (ID >= 42 && ID <= 55) {cdcode = (ID + 400) - 41;}
			else if (ID >= 56 && ID <= 58) {cdcode = (ID + 500) - 55;}
			

			
			// Create cells within neighborhood
			for (int i=nb_minX; i < nb_maxX; i++) {
				for (int j=nb_minY; j < nb_maxY; j++) {
					AlcoholCell newSpace = new AlcoholCell(i, j, ID);
					hoodSpace.putObjectAt(i, j, newSpace);
					neighborhoodCellList.add(newSpace);
				}
			}
			// System.out.printf("Created %d cells in neighborhood %d \n", neighborhoodCellList.size(), ID);
			

			// System.out.printf("Created %d outlets in neighborhood %d \n", neighborhoodOutletList.size(), ID);
			
			/*
			// Double check creation of alcohol outlets
			int numOnPrem = 0;
			int numOffPrem = 0;
			for (int i=0; i<neighborhoodCellList.size(); i++) {
				AlcoholCell nextCell = (AlcoholCell)neighborhoodCellList.get(i);
				if (nextCell.getOnPremOutlet() == 1) numOnPrem += 1;
				if (nextCell.getOffPremOutlet() == 1) numOffPrem += 1;
			}
			System.out.printf("Created %d on- and %d off-premise outlets in neighborhood %d \n", numOnPrem, numOffPrem, ID);
			
			int numOnPrem2 = 0;
			int numOffPrem2 = 0;
			for (int i=0; i<neighborhoodCellList.size(); i++) {
				AlcoholCell nextCell = (AlcoholCell)neighborhoodCellList.get(i);
				if (nextCell.getOnPremOutlet() == 1) { 
					AlcoholOutlet onOutlet = nextCell.getMyOnOutlet();
					if (onOutlet.getOnPremise()==1) { numOnPrem2 += 1; }
				}
				if (nextCell.getOffPremOutlet() == 1) {
					AlcoholOutlet offOutlet = nextCell.getMyOffOutlet();
					if (offOutlet.getOffPremise()==1) { numOffPrem2 += 1; }
				}
			}
			System.out.printf("Created %d on- and %d off-premise outlets in neighborhood %d \n", numOnPrem2, numOffPrem2, ID);
			*/
			
			// Assign police officers to neighborhood (including initial number and random locations)

			/*
			// Double check assignment of police officers
			int numOfficer = 0;
			for (int i=0; i<neighborhoodCellList.size(); i++) {
				AlcoholCell c = (AlcoholCell)neighborhoodCellList.get(i);
				if (c.getPoliceOfficer() == 1) numOfficer += 1;
			}
			System.out.printf("Created %d of %d police officers in neighborhood %d \n", numOfficer, numPolice, ID);
			*/
			
			// assign neighborhood composition variables from external (census) data
			hoodFborn();
			hoodManProf();
			hoodUnemp();
			hoodFemHHKids();
			
			// initialize neighborhood variables to have values of -1 or 0
			hoodinc = -1;
			hoodinc1 = -1;
			hoodinc2 = -1;
			highhoodinc = -1;
			highhoodviol = -1;
			numviolevent = 0;
			targetHood = 0;
			percBlack = 0;
			percHisp = 0;
			avgAge = -1;
			percStable = 0;
			percLightDrk = 0;
			percHeavyDrk = 0;
			percYoungMale = 0;

			
			
		} // end of Neighborhood constructor
		
		//////////////////////// FUNCTIONS CALLED IN NEIGHBORHOOD CONSTRUCTOR
				
		// hoodOnPremise()
		// Assign proportion of cells with on-premise liquor outlets
		// Using data from the New York State Liquor Authority (as of Nov. 2002)
		

		
		// hoodFborn()
		// Assign percent foreign-born in each neighborhood, from 2000 U.S. Census data
		public void hoodFborn() {
			if (cdcode ==	101	) { percFBorn =	21.70	; }
			else if (cdcode ==	102	) { percFBorn =	27.25	; }
			else if (cdcode ==	103	) { percFBorn =	19.80	; }
			else if (cdcode ==	104	) { percFBorn =	35.30	; }
			else if (cdcode ==	105	) { percFBorn =	34.83	; }
			else if (cdcode ==	106	) { percFBorn =	23.50	; }
			else if (cdcode ==	107	) { percFBorn =	36.68	; }
			else if (cdcode ==	108	) { percFBorn =	30.02	; }
			else if (cdcode ==	109	) { percFBorn =	24.35	; }
			else if (cdcode ==	110	) { percFBorn =	17.44	; }
			else if (cdcode ==	111	) { percFBorn =	30.16	; }
			else if (cdcode ==	112	) { percFBorn =	37.13	; }
			else if (cdcode ==	201	) { percFBorn =	32.55	; }
			else if (cdcode ==	202	) { percFBorn =	16.33	; }
			else if (cdcode ==	203	) { percFBorn =	18.53	; }
			else if (cdcode ==	204	) { percFBorn =	34.82	; }
			else if (cdcode ==	205	) { percFBorn =	32.97	; }
			else if (cdcode ==	206	) { percFBorn =	16.40	; }
			else if (cdcode ==	207	) { percFBorn =	42.70	; }
			else if (cdcode ==	208	) { percFBorn =	30.61	; }
			else if (cdcode ==	209	) { percFBorn =	46.22	; }
			else if (cdcode ==	210	) { percFBorn =	37.07	; }
			else if (cdcode ==	211	) { percFBorn =	50.78	; }
			else if (cdcode ==	212	) { percFBorn =	41.66	; }
			else if (cdcode ==	213	) { percFBorn =	47.31	; }
			else if (cdcode ==	214	) { percFBorn =	49.76	; }
			else if (cdcode ==	215	) { percFBorn =	45.38	; }
			else if (cdcode ==	216	) { percFBorn =	21.21	; }
			else if (cdcode ==	217	) { percFBorn =	54.71	; }
			else if (cdcode ==	218	) { percFBorn =	36.99	; }
			else if (cdcode ==	301	) { percFBorn =	24.39	; }
			else if (cdcode ==	302	) { percFBorn =	23.60	; }
			else if (cdcode ==	303	) { percFBorn =	40.11	; }
			else if (cdcode ==	304	) { percFBorn =	25.09	; }
			else if (cdcode ==	305	) { percFBorn =	25.26	; }
			else if (cdcode ==	306	) { percFBorn =	24.17	; }
			else if (cdcode ==	307	) { percFBorn =	22.06	; }
			else if (cdcode ==	308	) { percFBorn =	21.48	; }
			else if (cdcode ==	309	) { percFBorn =	35.73	; }
			else if (cdcode ==	310	) { percFBorn =	17.76	; }
			else if (cdcode ==	311	) { percFBorn =	21.02	; }
			else if (cdcode ==	312	) { percFBorn =	53.29	; }
			else if (cdcode ==	401	) { percFBorn =	49.25	; }
			else if (cdcode ==	402	) { percFBorn =	60.28	; }
			else if (cdcode ==	403	) { percFBorn =	61.67	; }
			else if (cdcode ==	404	) { percFBorn =	67.86	; }
			else if (cdcode ==	405	) { percFBorn =	36.04	; }
			else if (cdcode ==	406	) { percFBorn =	51.69	; }
			else if (cdcode ==	407	) { percFBorn =	50.29	; }
			else if (cdcode ==	408	) { percFBorn =	44.43	; }
			else if (cdcode ==	409	) { percFBorn =	48.64	; }
			else if (cdcode ==	410	) { percFBorn =	39.27	; }
			else if (cdcode ==	411	) { percFBorn =	35.88	; }
			else if (cdcode ==	412	) { percFBorn =	34.41	; }
			else if (cdcode ==	413	) { percFBorn =	38.41	; }
			else if (cdcode ==	414	) { percFBorn =	24.41	; }
			else if (cdcode ==	501	) { percFBorn =	19.17	; }
			else if (cdcode ==	502	) { percFBorn =	18.41	; }
			else if (cdcode ==	503	) { percFBorn =	11.70	; }
		} // end of hoodFborn()
		
		// hoodManProf()
		// Assign percent in managerial/professional occupations in each neighborhood, from 2000 U.S. Census data
		public void hoodManProf() {
			if (cdcode ==	101	) { percManProf =	2.98	; }
			else if (cdcode ==	102	) { percManProf =	4.00	; }
			else if (cdcode ==	103	) { percManProf =	2.73	; }
			else if (cdcode ==	104	) { percManProf =	2.57	; }
			else if (cdcode ==	105	) { percManProf =	2.35	; }
			else if (cdcode ==	106	) { percManProf =	2.46	; }
			else if (cdcode ==	107	) { percManProf =	4.20	; }
			else if (cdcode ==	108	) { percManProf =	8.09	; }
			else if (cdcode ==	109	) { percManProf =	4.17	; }
			else if (cdcode ==	110	) { percManProf =	5.75	; }
			else if (cdcode ==	111	) { percManProf =	4.43	; }
			else if (cdcode ==	112	) { percManProf =	4.79	; }
			else if (cdcode ==	201	) { percManProf =	6.59	; }
			else if (cdcode ==	202	) { percManProf =	15.53	; }
			else if (cdcode ==	203	) { percManProf =	4.13	; }
			else if (cdcode ==	204	) { percManProf =	3.38	; }
			else if (cdcode ==	205	) { percManProf =	3.39	; }
			else if (cdcode ==	206	) { percManProf =	15.77	; }
			else if (cdcode ==	207	) { percManProf =	6.20	; }
			else if (cdcode ==	208	) { percManProf =	6.41	; }
			else if (cdcode ==	209	) { percManProf =	3.82	; }
			else if (cdcode ==	210	) { percManProf =	7.48	; }
			else if (cdcode ==	211	) { percManProf =	6.62	; }
			else if (cdcode ==	212	) { percManProf =	5.32	; }
			else if (cdcode ==	213	) { percManProf =	5.70	; }
			else if (cdcode ==	214	) { percManProf =	6.24	; }
			else if (cdcode ==	215	) { percManProf =	7.10	; }
			else if (cdcode ==	216	) { percManProf =	3.29	; }
			else if (cdcode ==	217	) { percManProf =	3.51	; }
			else if (cdcode ==	218	) { percManProf =	5.23	; }
			else if (cdcode ==	301	) { percManProf =	20.29	; }
			else if (cdcode ==	302	) { percManProf =	19.57	; }
			else if (cdcode ==	303	) { percManProf =	10.78	; }
			else if (cdcode ==	304	) { percManProf =	18.03	; }
			else if (cdcode ==	305	) { percManProf =	22.38	; }
			else if (cdcode ==	306	) { percManProf =	20.60	; }
			else if (cdcode ==	307	) { percManProf =	18.71	; }
			else if (cdcode ==	308	) { percManProf =	19.92	; }
			else if (cdcode ==	309	) { percManProf =	7.16	; }
			else if (cdcode ==	310	) { percManProf =	5.31	; }
			else if (cdcode ==	311	) { percManProf =	5.58	; }
			else if (cdcode ==	312	) { percManProf =	5.39	; }
			else if (cdcode ==	401	) { percManProf =	7.46	; }
			else if (cdcode ==	402	) { percManProf =	7.21	; }
			else if (cdcode ==	403	) { percManProf =	4.63	; }
			else if (cdcode ==	404	) { percManProf =	4.02	; }
			else if (cdcode ==	405	) { percManProf =	6.09	; }
			else if (cdcode ==	406	) { percManProf =	12.75	; }
			else if (cdcode ==	407	) { percManProf =	7.07	; }
			else if (cdcode ==	408	) { percManProf =	7.66	; }
			else if (cdcode ==	409	) { percManProf =	6.38	; }
			else if (cdcode ==	410	) { percManProf =	5.42	; }
			else if (cdcode ==	411	) { percManProf =	8.63	; }
			else if (cdcode ==	412	) { percManProf =	3.90	; }
			else if (cdcode ==	413	) { percManProf =	5.02	; }
			else if (cdcode ==	414	) { percManProf =	4.35	; }
			else if (cdcode ==	501	) { percManProf =	6.48	; }
			else if (cdcode ==	502	) { percManProf =	6.82	; }
			else if (cdcode ==	503	) { percManProf =	6.82	; }
		} // end of hoodManProf()
		
		// hoodUnemp()
		// Assign proportion unemployed in each neighborhood, from 2000 U.S. Census
		public void hoodUnemp() {
			if (cdcode ==	101	) { percUnemp = 	0.239056004	; }
			else if (cdcode ==	102	) { percUnemp = 	0.23804333	; }
			else if (cdcode ==	103	) { percUnemp = 	0.218621679	; }
			else if (cdcode ==	104	) { percUnemp = 	0.181693429	; }
			else if (cdcode ==	105	) { percUnemp = 	0.200382897	; }
			else if (cdcode ==	106	) { percUnemp = 	0.206879465	; }
			else if (cdcode ==	107	) { percUnemp = 	0.149455771	; }
			else if (cdcode ==	108	) { percUnemp = 	0.093840797	; }
			else if (cdcode ==	109	) { percUnemp = 	0.141445666	; }
			else if (cdcode ==	110	) { percUnemp = 	0.065352499	; }
			else if (cdcode ==	111	) { percUnemp = 	0.085037205	; }
			else if (cdcode ==	112	) { percUnemp = 	0.105679723	; }
			else if (cdcode ==	201	) { percUnemp = 	0.106102408	; }
			else if (cdcode ==	202	) { percUnemp = 	0.099762109	; }
			else if (cdcode ==	203	) { percUnemp = 	0.180239994	; }
			else if (cdcode ==	204	) { percUnemp = 	0.169613465	; }
			else if (cdcode ==	205	) { percUnemp = 	0.162748962	; }
			else if (cdcode ==	206	) { percUnemp = 	0.055426491	; }
			else if (cdcode ==	207	) { percUnemp = 	0.083532814	; }
			else if (cdcode ==	208	) { percUnemp = 	0.142484209	; }
			else if (cdcode ==	209	) { percUnemp = 	0.13512394	; }
			else if (cdcode ==	210	) { percUnemp = 	0.059996568	; }
			else if (cdcode ==	211	) { percUnemp = 	0.071361137	; }
			else if (cdcode ==	212	) { percUnemp = 	0.07591611	; }
			else if (cdcode ==	213	) { percUnemp = 	0.105203863	; }
			else if (cdcode ==	214	) { percUnemp = 	0.109363652	; }
			else if (cdcode ==	215	) { percUnemp = 	0.06734187	; }
			else if (cdcode ==	216	) { percUnemp = 	0.228641888	; }
			else if (cdcode ==	217	) { percUnemp = 	0.125976904	; }
			else if (cdcode ==	218	) { percUnemp = 	0.079770946	; }
			else if (cdcode ==	301	) { percUnemp = 	0.077729526	; }
			else if (cdcode ==	302	) { percUnemp = 	0.050798642	; }
			else if (cdcode ==	303	) { percUnemp = 	0.094488189	; }
			else if (cdcode ==	304	) { percUnemp = 	0.068985128	; }
			else if (cdcode ==	305	) { percUnemp = 	0.073307488	; }
			else if (cdcode ==	306	) { percUnemp = 	0.042781212	; }
			else if (cdcode ==	307	) { percUnemp = 	0.050553118	; }
			else if (cdcode ==	308	) { percUnemp = 	0.037082616	; }
			else if (cdcode ==	309	) { percUnemp = 	0.179983576	; }
			else if (cdcode ==	310	) { percUnemp = 	0.183986164	; }
			else if (cdcode ==	311	) { percUnemp = 	0.170458589	; }
			else if (cdcode ==	312	) { percUnemp = 	0.145225916	; }
			else if (cdcode ==	401	) { percUnemp = 	0.07789514	; }
			else if (cdcode ==	402	) { percUnemp = 	0.071818001	; }
			else if (cdcode ==	403	) { percUnemp = 	0.098798845	; }
			else if (cdcode ==	404	) { percUnemp = 	0.094107788	; }
			else if (cdcode ==	405	) { percUnemp = 	0.072727273	; }
			else if (cdcode ==	406	) { percUnemp = 	0.052124345	; }
			else if (cdcode ==	407	) { percUnemp = 	0.055314018	; }
			else if (cdcode ==	408	) { percUnemp = 	0.06198662	; }
			else if (cdcode ==	409	) { percUnemp = 	0.082748245	; }
			else if (cdcode ==	410	) { percUnemp = 	0.070215056	; }
			else if (cdcode ==	411	) { percUnemp = 	0.040852791	; }
			else if (cdcode ==	412	) { percUnemp = 	0.10827852	; }
			else if (cdcode ==	413	) { percUnemp = 	0.072244103	; }
			else if (cdcode ==	414	) { percUnemp = 	0.127718466	; }
			else if (cdcode ==	501	) { percUnemp = 	0.082084629	; }
			else if (cdcode ==	502	) { percUnemp = 	0.050624633	; }
			else if (cdcode ==	503	) { percUnemp = 	0.041798684	; }
		}
		
		// hoodFemHHKids() 
		// Assign proportion of female-headed households with children under 18 yrs old, from 2000 U.S. Census
		// 11.13.2015 -- corrected values assigned here (previous version repeated unemployment assignment)
		public void hoodFemHHKids () {
			if (cdcode ==	101	) { percFemHHKids = 	0.252644818	; }
			else if (cdcode ==	102	) { percFemHHKids = 	0.258508526	; }
			else if (cdcode ==	103	) { percFemHHKids = 	0.274661077	; }
			else if (cdcode ==	104	) { percFemHHKids = 	0.251452354	; }
			else if (cdcode ==	105	) { percFemHHKids = 	0.282641631	; }
			else if (cdcode ==	106	) { percFemHHKids = 	0.274036544	; }
			else if (cdcode ==	107	) { percFemHHKids = 	0.216658342	; }
			else if (cdcode ==	108	) { percFemHHKids = 	0.091473392	; }
			else if (cdcode ==	109	) { percFemHHKids = 	0.202170757	; }
			else if (cdcode ==	110	) { percFemHHKids = 	0.081092919	; }
			else if (cdcode ==	111	) { percFemHHKids = 	0.10884926	; }
			else if (cdcode ==	112	) { percFemHHKids = 	0.162672864	; }
			else if (cdcode ==	201	) { percFemHHKids = 	0.094196533	; }
			else if (cdcode ==	202	) { percFemHHKids = 	0.079732141	; }
			else if (cdcode ==	203	) { percFemHHKids = 	0.221187473	; }
			else if (cdcode ==	204	) { percFemHHKids = 	0.229335623	; }
			else if (cdcode ==	205	) { percFemHHKids = 	0.223823893	; }
			else if (cdcode ==	206	) { percFemHHKids = 	0.067773404	; }
			else if (cdcode ==	207	) { percFemHHKids = 	0.092984736	; }
			else if (cdcode ==	208	) { percFemHHKids = 	0.179426481	; }
			else if (cdcode ==	209	) { percFemHHKids = 	0.172189909	; }
			else if (cdcode ==	210	) { percFemHHKids = 	0.03798134	; }
			else if (cdcode ==	211	) { percFemHHKids = 	0.047743909	; }
			else if (cdcode ==	212	) { percFemHHKids = 	0.047751153	; }
			else if (cdcode ==	213	) { percFemHHKids = 	0.09459619	; }
			else if (cdcode ==	214	) { percFemHHKids = 	0.125163245	; }
			else if (cdcode ==	215	) { percFemHHKids = 	0.04677514	; }
			else if (cdcode ==	216	) { percFemHHKids = 	0.282505869	; }
			else if (cdcode ==	217	) { percFemHHKids = 	0.181951604	; }
			else if (cdcode ==	218	) { percFemHHKids = 	0.11711739	; }
			else if (cdcode ==	301	) { percFemHHKids = 	0.032185033	; }
			else if (cdcode ==	302	) { percFemHHKids = 	0.014760919	; }
			else if (cdcode ==	303	) { percFemHHKids = 	0.065689209	; }
			else if (cdcode ==	304	) { percFemHHKids = 	0.021704791	; }
			else if (cdcode ==	305	) { percFemHHKids = 	0.010516619	; }
			else if (cdcode ==	306	) { percFemHHKids = 	0.015548135	; }
			else if (cdcode ==	307	) { percFemHHKids = 	0.033471878	; }
			else if (cdcode ==	308	) { percFemHHKids = 	0.019535706	; }
			else if (cdcode ==	309	) { percFemHHKids = 	0.131245452	; }
			else if (cdcode ==	310	) { percFemHHKids = 	0.189759762	; }
			else if (cdcode ==	311	) { percFemHHKids = 	0.174336588	; }
			else if (cdcode ==	312	) { percFemHHKids = 	0.158827192	; }
			else if (cdcode ==	401	) { percFemHHKids = 	0.064288804	; }
			else if (cdcode ==	402	) { percFemHHKids = 	0.046789857	; }
			else if (cdcode ==	403	) { percFemHHKids = 	0.07808299	; }
			else if (cdcode ==	404	) { percFemHHKids = 	0.074066747	; }
			else if (cdcode ==	405	) { percFemHHKids = 	0.073130902	; }
			else if (cdcode ==	406	) { percFemHHKids = 	0.02836959	; }
			else if (cdcode ==	407	) { percFemHHKids = 	0.04192957	; }
			else if (cdcode ==	408	) { percFemHHKids = 	0.057627682	; }
			else if (cdcode ==	409	) { percFemHHKids = 	0.078486644	; }
			else if (cdcode ==	410	) { percFemHHKids = 	0.070096668	; }
			else if (cdcode ==	411	) { percFemHHKids = 	0.03298946	; }
			else if (cdcode ==	412	) { percFemHHKids = 	0.130343248	; }
			else if (cdcode ==	413	) { percFemHHKids = 	0.080299803	; }
			else if (cdcode ==	414	) { percFemHHKids = 	0.142914936	; }
			else if (cdcode ==	501	) { percFemHHKids = 	0.112144153	; }
			else if (cdcode ==	502	) { percFemHHKids = 	0.045675328	; }
			else if (cdcode ==	503	) { percFemHHKids = 	0.037290558	; }
		}
		
		///////////////////////////// setters and getters

		public int getID() { return ID; }	
		public void setID( int i ) { ID = i; }

		public int getnb_minX() { return nb_minX; }	
		public void setnb_minX( int i ) { nb_minX = i; }

		public int getnb_minY() { return nb_minY; }	
		public void setnb_minY( int i ) { nb_minY = i; }

		public int getnb_maxX() { return nb_maxX; }	
		public void setnb_maxX( int i ) { nb_maxX = i; }

		public int getnb_maxY() { return nb_maxY; }	
		public void setnb_maxY( int i ) { nb_maxY = i; }
		
		public int getCdcode() { return cdcode; }
		public void setCdcode( int i) { this.cdcode = i; }

		public double getAvghoodinc() { return avghoodinc; }
		public void setAvghoodinc ( double i ) { avghoodinc=i; }
		
		public double getLastavghoodinc() { return lastavghoodinc; }
		public void setLastavghoodinc ( double i ) { lastavghoodinc=i; }

		public int getHoodinc(){ return hoodinc; }
		public void setHoodinc( int i ){ this.hoodinc=i; }

		public int getHoodinc1(){ return hoodinc1; }
		public void setHoodinc1( int i ){ this.hoodinc1=i; }

		public int getHoodinc2(){ return hoodinc2; }
		public void setHoodinc2( int i ){ this.hoodinc2=i; }

		public int getHighhoodinc() { return highhoodinc; }
		public void setHighhoodinc( int i) { this.highhoodinc=i; }
		
		public double getPercBlack() { return percBlack; }
		public void setPercBlack( double i) { this.percBlack=i; }
		
		public double getPercHisp() { return percHisp; }
		public void setPercHisp( double i) { this.percHisp=i; }
		
		public double getAvgAge() { return avgAge; }
		public void setAvgAge( double i) { this.avgAge=i; }
		
		public double getPercYoungMale() { return percYoungMale; }
		public void setPercYoungMale( double i) { this.percYoungMale = i; }
		
		public double getPercStable() { return percStable; }
		public void setPercStable( double i) { this.percStable=i; }
		
		public double getPercFBorn() { return percFBorn; }
		public void setPercFBorn( double i) { this.percFBorn=i; }
		
		public double getPercManProf() { return percManProf; }
		public void setPercManProf( double i) { this.percManProf=i; }
		
		public double getPercUnemp() { return percUnemp; }
		public void setPercUnemp( double i) { this.percUnemp = i; }
		
		public double getPercFemHHKids() { return percFemHHKids; }
		public void setPercFemHHKids( double i) { this.percFemHHKids = i; }
		
		public double getPercLightDrk() { return percLightDrk; }
		public void setPercLightDrk( double i) { this.percLightDrk=i; }
		
		public double getPercHeavyDrk() { return percHeavyDrk; }
		public void setPercHeavyDrk( double i) { this.percHeavyDrk=i; }

		public double getAvghoodviol() { return avghoodviol; }
		public void setAvghoodviol ( double i) { avghoodviol=i; }
		
		public int getNumviolevent() { return numviolevent; }
		public void setNumviolevent( int i) { this.numviolevent=i; }
		
		public double getAvghoodperp() { return avghoodperp; }
		public void setAvghoodperp ( double i) { avghoodperp=i; }

		public double getLastavghoodviol() { return lastavghoodviol; }
		public void setLastavghoodviol ( double i) { lastavghoodviol=i; }
		
		public double getHomrate() { return homrate; }
		public void setHomrate( double i) { this.homrate=i; }
		
		public double getAlchomrate() { return alchomrate; }
		public void setAlchomrate( double i) { this.alchomrate=i; }

		public int getHighhoodviol(){ return highhoodviol; }
		public void setHighhoodviol( int i) { this.highhoodviol=i; }

		public double getChangeViol() { return changeviol; }
		public void setChangeViol( double i) { this.changeviol=i; }
		
		public double getChangeIncome() { return changeincome; }
		public void setChangeIncome( double i) { this.changeincome=i; }
		
		public int getTargetHood() { return targetHood; }
		public void setTargetHood(int i) { this.targetHood=i; }
		
		public ArrayList<AlcoholAgent> getNeighborhoodAgentList(){return neighborhoodAgentList;}
		public void setNeighborhoodAgentList(ArrayList<AlcoholAgent> i){ neighborhoodAgentList=i;}
		
		public ArrayList<AlcoholAgent> getTemphoodAgentList(){return temphoodAgentList;}
		public void setTemphoodAgentList(ArrayList<AlcoholAgent> i){ temphoodAgentList=i;}
		
		public ArrayList<AlcoholCell> getNeighborhoodCellList() { return neighborhoodCellList; }
		public void setNeighborhoodCellList(ArrayList<AlcoholCell> i) { neighborhoodCellList = i; }
		
		
		public int getHoodSize() { return neighborhoodAgentList.size(); }
		public void setHoodSize( int i) { this.hoodSize = i; }

} // end of Neighborhood class