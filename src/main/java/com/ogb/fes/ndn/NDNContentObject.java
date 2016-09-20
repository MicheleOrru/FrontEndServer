package com.ogb.fes.ndn;

import net.named_data.jndn.Data;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SignedBlob;

public class NDNContentObject 
{
	Name nameURI;
	byte[] content;
	byte[] unsignedContent;
	byte[] signedContent;

	
	public NDNContentObject(byte[] content, Name nameURI, KeyChain keyChain, Name keyLocator, boolean isFinal) throws SecurityException {
		super();
		
		this.content = content;
		this.nameURI = nameURI;

		Data data = new Data();
		data.setName(nameURI);
		data.setContent(new Blob(content));
		if (isFinal) {
			data.getMetaInfo().setFinalBlockId(data.getName().get(-1));
		//	System.out.println("Added final block id:" + data.getName().get(-1).toEscapedString());
		}
		
		SignedBlob unsigned = data.wireEncode();
		unsignedContent = unsigned.getImmutableArray();
		
		keyChain.sign(data, keyLocator);
		SignedBlob signed = data.wireEncode();
		signedContent = signed.getImmutableArray();
	}
	
	public byte[] getSignedContent() {
		return signedContent;
	}

	public  byte[] getUnsignedContent() {
		return unsignedContent;
	}
	
	public Name getNameURI() {
		return nameURI;
	}
}
