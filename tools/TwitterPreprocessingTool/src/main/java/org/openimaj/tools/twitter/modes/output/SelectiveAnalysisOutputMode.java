/**
 * Copyright (c) 2012, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.tools.twitter.modes.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.twitter.TwitterStatus;

/**
 * An ouput mode which alters the tweets being outputted
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>, Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class SelectiveAnalysisOutputMode implements TwitterOutputMode{
	private List<String> selectiveAnalysis;
	private String delim = null;
	private List<String> twitterExtras;

	/**
	 * Non selective, output everything 
	 */
	public SelectiveAnalysisOutputMode() {
		this.selectiveAnalysis = new ArrayList<String>();
		this.twitterExtras = new ArrayList<String>();
	}
	
	/**
	 * Only output the analysis keys given, dump the rest of the tweet. If the selectiveAnalysis is empty,
	 * the whole tweet is outputted.
	 * 
	 * @param selectiveAnalysis
	 */
	public SelectiveAnalysisOutputMode(List<String> selectiveAnalysis) {
		this.selectiveAnalysis = selectiveAnalysis;
		this.twitterExtras = new ArrayList<String>();
	}

	/**
	 * Selectively save certain analysis and certain status information
	 * @param selectiveAnalysis the analysis to save
	 * @param twitterExtras the status information tos ave
	 */
	public SelectiveAnalysisOutputMode(List<String> selectiveAnalysis,List<String> twitterExtras) {
		this.selectiveAnalysis = selectiveAnalysis;
		this.twitterExtras = twitterExtras;
	}

	@Override
	public void output(TwitterStatus twitterStatus, PrintWriter outputWriter) throws IOException {
		if(this.selectiveAnalysis.isEmpty()){
			twitterStatus.writeASCII(outputWriter);
		}
		else{
			if(this.twitterExtras.isEmpty()){
				twitterStatus.writeASCIIAnalysis(outputWriter,this.selectiveAnalysis);
			}
			else{
				twitterStatus.writeASCIIAnalysis(outputWriter,this.selectiveAnalysis,twitterExtras);
			}
		}
		if(delim != null){
			outputWriter.print(this.delim);
		}
	}

	@Override
	public void deliminate(String string) {
		this.delim  = string;
	}
	
}
