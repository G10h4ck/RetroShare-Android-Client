package com.example.retroshare.remote;

public interface UiThreadHandlerInterface {
	/**
	 * Posts a Runnable Object to the UI Thread and calls run() in UI Thread
	 * @param r Runnable to run in UI Thread
	 */
	public void postToUiThread(Runnable r);
}
