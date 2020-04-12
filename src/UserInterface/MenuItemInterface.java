package UserInterface;

import java.util.Scanner;

import Exceptions.SaveObjectException;

interface MenuItemInterface
{
	public void applyChange(String line) throws IllegalArgumentException, SaveObjectException;
	public String prevValue();
}