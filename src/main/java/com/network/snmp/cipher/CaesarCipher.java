package com.network.snmp.cipher;

import java.util.HashMap; import java.util.Scanner;

public class CaesarCipher{

    HashMap<Character,Integer> alphaToNumeric= new HashMap<>(); HashMap<Integer,Character> numericToAlphaUpper = new HashMap<>(); HashMap<Integer,Character> numericToAlphaLower = new HashMap<>();

    public void AssignAlphabets(){ for(int i=0;i<26;i++){
        char upper = (char)('A'+i); char lower = (char) ('a'+i); alphaToNumeric.put(upper,i); alphaToNumeric.put(lower,i);
        numericToAlphaUpper.put(i,upper); numericToAlphaLower.put(i,lower);
    }
    }

    public String CaesarEncryption(String plainText,int shiftValue){ StringBuilder sb = new StringBuilder();
        for(char character:plainText.toCharArray()){ if(!alphaToNumeric.containsKey(character)){
            sb.append(character); continue;}
            int newIndex = (alphaToNumeric.get(character)+shiftValue)%26; if(Character.isUpperCase(character)){
                sb.append(numericToAlphaUpper.get(newIndex));
            }else{
                sb.append(numericToAlphaLower.get(newIndex));
            }
        }
        return sb.toString();
    }

    public String CaesarDecryption(String cipherText,int shiftValue){ StringBuilder sb = new StringBuilder();
        for(char character:cipherText.toCharArray()){ if(!alphaToNumeric.containsKey(character)){
            sb.append(character); continue;}
            int newIndex = (((alphaToNumeric.get(character)-shiftValue)+26)%26);

            if(Character.isUpperCase(character)){ sb.append(numericToAlphaUpper.get(newIndex));
            }else{
                sb.append(numericToAlphaLower.get(newIndex));
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) { CaesarCipher algo = new CaesarCipher(); Scanner toGetInput = new Scanner(System.in); algo.AssignAlphabets();
        System.out.print("Enter the String to apply Caesar Algorithm: "); String plainText = toGetInput.nextLine();
        System.out.print("Enter the Shift Value for Casear Algorithm: "); int shiftValue = toGetInput.nextInt();
        System.out.println("The entered Plain Text is: "+plainText+" and Shift Value is: "+shiftValue);
        String cipherText = algo.CaesarEncryption(plainText,shiftValue); System.out.println("Encrypter Cipher text is: "+cipherText); System.out.println("Decrypted Plain text is: c"+algo.CaesarDecryption(cipherText,shiftValue));
    }
}
