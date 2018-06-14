import com.credits.crypto.Ed25519;
import com.credits.leveldb.client.data.TransactionFlowData;
import com.credits.wallet.desktop.App;
import com.credits.wallet.desktop.AppState;
import com.credits.wallet.desktop.utils.ApiUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.credits.common.utils.Converter;

public class Main {

	public static String PUBLIC_KEY = "";
	public static String PRIVATE_KEY = "";
	public static HashMap<String, PrivateKey> PRIVATE_KEYS = new HashMap<String, PrivateKey>();
	public static ArrayList<String[]> SOURCE_WALLETS = new ArrayList<String[]>();
	public static int AMOUNT_OF_THREADS = 10;
	public static long NUMBER_OF_TXS_TO_SEND = 10000;

	public static int random(int min, int max) {
		return min + RandomGenerator.nextInt(max - min + 1);
	}

	public static void callTransactionFlow(String innerId, String source, String target, PrivateKey sourcePrviateKey, BigDecimal amount,
			BigDecimal balance, String currency) throws Exception {
		String signature = Ed25519.generateSignOfTransaction(innerId, source, target, amount, balance, currency,
				sourcePrviateKey);

		TransactionFlowData transactionFlowData = new TransactionFlowData(innerId, source, target, amount, balance,
				currency, signature);

		AppState.apiClient.transactionFlow(transactionFlowData, false);
	}

	private static String[] WALLETS_TO_SEND = { "BqNx1bUN77f7mx8aDsNu7MFeNdZq74ddDkByHrEPzYjL",
			"3mAu1HX9EK9oP5TqPgLRQHZwkgdKnE2dghftVcjGviCZ", "5Q7416un7B4HZpLLLz7jy3EmUpkgUiBxvDKaEgMMCxWC",
			"3TTvcL7qSJjEpzq4iZ66MLXxqgWdZFX4oW67SjQX6rDt", "6KUF6e4C1MvtysHrUmMRpb93Na8ToZXdZd7fJFy1REUe",
			"Hk4EwSsBSh1EU35gDiqSWFLv4Aus5nkNCvAzwYF5o7RH", "2CsYfVkdiK7H9mfEqhSKTdgjWzqsdHJFVNboNnek5ATc",
			"HYw7dqSAq8ytyjnaf8RhFnkdsHfjB8ezfLkcuGRCpjQW", "67mMej6sRHfuxk1CkJKqY7VDetGucUQQvMVUyt3RXr8p",
			"GRGykjJtRvWSbBBGGhubQa5fy89e8uq9bwSVLXPrEamh", "BwYFLA7ByVhuznFM6EmTDjosMfyteJr4cs3MKTAXdKsA" };

	public static void main(String[] args) throws Exception {
		File keyFile = new File("./keys");
		if (!keyFile.exists()) {
			System.out.println("Missing 'keys' file");
			System.console().readLine();
			return;
		}
		Path keyPath = Paths.get(keyFile.toURI());
		List<String> lineList = Files.readAllLines(keyPath);
		String lines = "";
		for (String s : lineList)
			lines += s;

		JSONObject keyObject = new JSONObject(lines).getJSONObject("keys");
		PUBLIC_KEY = keyObject.getString("public");
		PRIVATE_KEY = keyObject.getString("private");
		System.out.println("Set PRIVATE_KEY=" + PRIVATE_KEY);
		System.out.println("Set PUBLIC_KEY=" + PUBLIC_KEY);

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				App.main(null);
			}
		});
		t.start();

		boolean invalid = true;
		while (invalid) {
			System.out.print("Enter no of tx's to send: ");
			try {
				long tx = Long.parseLong(System.console().readLine());
				NUMBER_OF_TXS_TO_SEND = tx;
			} catch (Exception e) {
				System.out.println("Invalid integer try again");
				invalid = true;
				continue;
			}
			invalid = false;
		}

		while (AppState.apiClient == null)
			;
		AppState.newAccount = false;
		open(PUBLIC_KEY, PRIVATE_KEY);
		BigDecimal balance = AppState.apiClient.getBalance(AppState.account, "cs");
		if(balance.doubleValue() < 1000) {
			System.out.println("You need atleast 1000 credits to run this test.");
			System.console().readLine();
			return;
		}
		
		System.out.println("Generating 10 source wallets (senders)");
		for (int i = 0; i < 10; i++) {
			SOURCE_WALLETS.add(makeAccount());
		}
		System.out.println("Generated source wallets attempting to send 100 CS to each.");
		Thread.sleep(3000);

		for (int i = 0; i < SOURCE_WALLETS.size(); i++) {
			balance = AppState.apiClient.getBalance(AppState.account, "cs");
			System.out.println("Wallet Balance: " + balance);
			final BigDecimal toSend = new BigDecimal("100");
			ApiUtils.callTransactionFlow(ApiUtils.generateTransactionInnerId(), PUBLIC_KEY, SOURCE_WALLETS.get(i)[0],
					toSend, balance, "cs");
		}
		Thread.sleep(3000);
		System.out.println("Assuming sending is success");

		
		System.out.println("Starting to send transactions....");
		Thread.sleep(1000);
		for (int i = 0; i < AMOUNT_OF_THREADS; i++) {
			Thread transactionThread = new Thread(() -> {
				try {
					sendTransactions();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			transactionThread.start();
		}
		
		System.out.println("Finished sending TX's");
		System.console().readLine();
	}

	public static String[] makeAccount() {
		KeyPair keyPair = Ed25519.generateKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		String publicStr = Converter.encodeToBASE58(Ed25519.publicKeyToBytes(publicKey));
		String privateStr = Converter.encodeToBASE58(Ed25519.privateKeyToBytes(privateKey));
		try {
			ApiUtils.execSystemTransaction(publicStr);
			PRIVATE_KEYS.put(publicStr, privateKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String[] { publicStr, privateStr };
	}

	private static void sendTransactions() throws Exception {
		for (int i = 0; i < NUMBER_OF_TXS_TO_SEND; i++) {
			int random = random(0, WALLETS_TO_SEND.length - 1);
			int randomSource = random(0, SOURCE_WALLETS.size() - 1);
			String to = WALLETS_TO_SEND[random];
			String[] from = SOURCE_WALLETS.get(randomSource);
			final BigDecimal balance = AppState.apiClient.getBalance(from[0], "cs");
			System.out.println("Wallet " + from + " Balance: " + balance);
			final BigDecimal toSend = new BigDecimal("0.0001");
			callTransactionFlow(ApiUtils.generateTransactionInnerId(), from[0], to, PRIVATE_KEYS.get(to), toSend, balance, "cs");
		}
	}

	private static void open(String pubKey, String privKey) {
		AppState.account = pubKey;
		if (AppState.newAccount) {
			try {
				ApiUtils.execSystemTransaction(pubKey);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		} else {
			try {
				byte[] publicKeyByteArr = Converter.decodeFromBASE58(pubKey);
				byte[] privateKeyByteArr = Converter.decodeFromBASE58(privKey);
				AppState.publicKey = Ed25519.bytesToPublicKey(publicKeyByteArr);
				AppState.privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr);
			} catch (Exception e) {
				if (e.getMessage() != null) {
					System.err.println(e.toString());
				}
			}
		}
		if (validateKeys(pubKey, privKey)) {
			System.out.println("Valid key. Login successful");
		} else {
			System.out.println("Key pair invalid");
		}
	}

	private static boolean validateKeys(String publicKey, String privateKey) {
		try {
			byte[] publicKeyByteArr = Converter.decodeFromBASE58(publicKey);
			byte[] privateKeyByteArr = Converter.decodeFromBASE58(privateKey);
			if (privateKeyByteArr.length <= 32) {
				return false;
			}
			for (int i = 0; (i < publicKeyByteArr.length) && (i < privateKeyByteArr.length - 32); i++) {
				if (publicKeyByteArr[i] != privateKeyByteArr[(i + 32)]) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
