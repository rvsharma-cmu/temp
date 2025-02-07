package rmi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

public class WorkerThreads<T> extends Thread {

	Socket clientSocket;

	Skeleton<T> skeleton;
	Class<T> classInterface;
	ObjectInputStream inputStream = null;
	ObjectOutputStream outputStream = null;
	T server;

	public WorkerThreads(Class<T> c, Skeleton<T> skeleton, Socket socket, T server) {

		this.clientSocket = socket;
		this.skeleton = skeleton;
		this.classInterface = c;
		this.server = server;
	}

	@Override
	public void run() {

		RequestObject requestData = null;
		ResponseObject responseData = null;
		InputStream inFromServer = null;
		DataInputStream in = null;
		OutputStream outToServer = null;
		try {
			if (this.skeleton.isServerStarted && clientSocket != null && !clientSocket.isClosed()) {
				
				//Create the input and output stream
				inFromServer = clientSocket.getInputStream();

				outToServer = clientSocket.getOutputStream();
				DataOutputStream out = new DataOutputStream(outToServer);
				in = new DataInputStream(inFromServer);
				outputStream = new ObjectOutputStream(out);
				outputStream.flush();
				inputStream = new ObjectInputStream(in);
				
				//recive the request data from client(deserialization data)
				requestData = (RequestObject) inputStream.readObject();
			}

		} catch (IOException | ClassNotFoundException e) {

			try {
				clientSocket.close();
			} catch (IOException e1) {

				e1.printStackTrace();
			}
			RMIException rmiExp = new RMIException("Can't create ObjectInputStream instance.", e);
			this.skeleton.service_error(rmiExp);
			e.printStackTrace();
		}

		if (outputStream != null)
			responseData = getResponseObject(requestData);

		if (responseData != null) {
			try {
				//send the data back to client (Serialize data)
				outputStream.writeObject(responseData);
				outputStream.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/*
	 * Generate the response object based on various params sent in the request
	 */
	private ResponseObject getResponseObject(RequestObject requestData) {

		Object respObject = null;
		if (requestData == null) {
			return null;
		}
		ResponseObject responseObject = new ResponseObject(null, null);

		String methodName = requestData.getMthdName();
		Class<?>[] argTypes = requestData.getArgTypes();
		Object[] objArguments = requestData.getArguments();

		Method method = null;
		try {
			method = server.getClass().getMethod(methodName, argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			RMIException rmiException = new RMIException("No such method", e);
			responseObject.setException(rmiException);
			return responseObject;
		}
		if (method != null) {
			method.setAccessible(true);

			try {
				respObject = method.invoke(server, objArguments);
			} catch (Exception e) {
				responseObject.setException(e);

				return responseObject;
			}

		}

		responseObject = new ResponseObject(respObject, null);

		return responseObject;

	}

}
