/*
 * Alcohol ABM
 *
 *     This model will compare interventions aimed at reducing racial disparities in alcohol-related
 * homicide, using New York City as the place and population of interest.
 * 
 *     The cell class represents one cell in the physical space where agents reside. The physical space can 
 * have some of its own attributes (e.g., alcohol outlets). Each cell may also contain one or more agents.
 * 
 * Revised March 21, 2014
 * 
 * Revisions:	(1) Added variables to indicate presence of on- and of off-premise alcohol outlet
 * 				(2) Randomly assigned alcohol outlets to cell locations within neighborhoods
 * 				(3) Created alcohol outlets for each cell, where appropriate, and linked outlets to cell - 3/26/2014
 */

package cbtModel;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.util.Random;

public class AlcoholCell implements Drawable {

	// variable declarations -- attributes of each cell in the physical space
	// and other components of the model that are relevant to the physical space
	private int myX, myY;			// x, y coordinates of cell
	private int potVictim;			// potential victim present on cell
	private int	realVictim;			// confirmed victim present on cell
	private AlcoholAgent myAgent;	// agent located on cell
	private int	agentIncome;		// income level of agent located on cell
	

	private int hoodID;				// ID number of neighborhood in which cell is located
	private int	highhoodinc;		// high level of income in neighborhood in which cell is located
	private int	highhoodviol;		// high level of violence in neighborhood in which cell is located
	
	public AlcoholCell(int x, int y, int ID, double onPremise, double offPremise, double bars) {
		myX = x;
		myY = y;
		hoodID = ID;

		// initialize variables
		potVictim = 0;
		realVictim = 0;




	}


	



	
	// Reset cell variables at start of each time step
	public void resetCellVars() {
		potVictim = 0;
		realVictim = 0;
	}

	/*
	// implement Drawable
	// Draw cells to indicate which neighborhoods are high vs low collective efficacy
	public void draw(SimGraphics g) {
		// TODO Auto-generated method stub
		{g.drawFastRect(Color.gray);}
		if (hoodID >= 0 && hoodID <= 11) {g.drawFastRect(Color.yellow);}
		else if (hoodID >= 12 && hoodID <= 29) {g.drawFastRect(Color.orange);}
		else if (hoodID >= 30 && hoodID <= 41) {g.drawFastRect(Color.blue);}
		else if (hoodID >= 42 && hoodID <= 55) {g.drawFastRect(Color.magenta);}
		else if (hoodID >= 56 && hoodID <= 58) {g.drawFastRect(Color.green);}
	}
	*/

	// implement Drawable
	// Draw cells to indicate presence of on- and off-premise alcohol outlet
	public void draw(SimGraphics g) {
		g.drawFastRect(Color.gray); 
	}

	///////////////////////////// setters and getters

	public int getX() { return myX; }

	public int getY() { return myY; }

	public int getHoodID() { return hoodID; }

	public int getPotVictim() { return potVictim; }
	public void setPotVictim( int i) { this.potVictim=i; }

	public int getRealVictim() { return realVictim; }
	public void setRealVictim( int i) { this.realVictim=i; }
	
	public AlcoholAgent getMyAgent() { return myAgent; }
	public void setMyAgent( AlcoholAgent va) { myAgent = va; }

	public int getAgentIcome() { return agentIncome; }
	public void setAgentIncome( int i) { this.agentIncome=i; }

	public int getHighhoodinc() { return highhoodinc; }
	public void setHighhoodinc( int i) { this.highhoodinc=i; }

	public int getHighhoodviol() { return highhoodviol; }
	public void setHighhoodviol( int i) { this.highhoodviol=i; }

} // end of violenceCell