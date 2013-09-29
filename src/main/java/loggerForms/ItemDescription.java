package loggerForms;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import PamDetection.NewPamDataUnit;
import PamView.PamButton;
import PamView.PamPanel;

import generalDatabase.EmptyTableDefinition;

/**
 * 
 * @author Graham Weatherup - SMRU
 *
 */
public abstract class ItemDescription {
	
	protected FormDescription formDescription;
	
	
	private Integer	id;
	private Integer	order;
	private String	type;
	private String	title;
	private String	postTitle;
	private String	dbTitle;
	private Integer	length;
	private String	topic;
	private String  nmeaModule;	
	private String	nmeaString;
	private Integer	nmeaPosition;
	private Boolean	required;
	private Integer autoUpdate;
	private Boolean	autoclear;
	private Boolean	forceGps;
	private String	hint;
	private Integer	adcChannel;
	private Integer	adcGain;
	private Float	analogueMultiply;
	private Float	analogueAdd;
	private Boolean	plot;
	private Integer	height;
	private String	colour;
	private Float	minValue;
	private Float	maxValue;
	private Boolean	readOnly;
	private String	sendControlName;
	private String	controlOnSubform;
	private String	getControlData;
	private String	defaultValue;
	
	private int itemErrors = 0;


	
	public ItemDescription(FormDescription formDescription){
		this.formDescription = formDescription;
		readTableDefRecord();
		
	}
	
	public ItemDescription(){
		
	}
	
	/**
	 * To be used by control Description for to check necessary fields exist
	 * @return null if ok otherwise warning string
	 */
	abstract public String getItemWarning();
	
	/**
	 * non static this means blank item description has to be created then a
	 * @return
	 */
	public JPanel getEditPanel(){
		
		JPanel editPanel = new PamPanel();
		GridLayout layout;
		editPanel.setLayout(layout=new GridLayout(29,2));
		
		editPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		
		
		JTextField id				= new JTextField(); id.setText				(String.valueOf(this.id));						//Integer	id;
		JTextField order			= new JTextField(); order.setText			(String.valueOf(this.order));					//Integer	order;
		JTextField type				= new JTextField(); type.setText			(String.valueOf(this.type));					//String	type;
		JTextField title			= new JTextField(); title.setText			(String.valueOf(this.title));					//String	title;
		JTextField postTitle		= new JTextField(); postTitle.setText		(String.valueOf(this.postTitle));				//String	postTitle;
		JTextField dbTitle			= new JTextField(); dbTitle.setText			(String.valueOf(this.dbTitle));					//String	dbTitle;
		JTextField length			= new JTextField(); length.setText			(String.valueOf(this.length));					//Integer	length;
		JTextField topic			= new JTextField(); topic.setText			(String.valueOf(this.topic));					//String	topic;
		JTextField nmeaModule		= new JTextField(); nmeaModule.setText		(String.valueOf(this.nmeaModule));				//String	nmeaModule;
		JTextField nmeaString		= new JTextField(); nmeaString.setText		(String.valueOf(this.nmeaString));				//String	nmeaString;
		JTextField nmeaPosition		= new JTextField(); nmeaPosition.setText	(String.valueOf(this.nmeaPosition));			//Integer	nmeaPosition;
		JCheckBox  required			= new JCheckBox();	required.setSelected	(this.required);								//Boolean	required;
		JTextField autoUpdate		= new JTextField(); autoUpdate.setText		(String.valueOf(this.autoUpdate));				//Integer autoUpdate;
		JCheckBox  autoclear		= new JCheckBox();	required.setSelected	(this.autoclear);								//Boolean	autoclear;
		JCheckBox  forceGps			= new JCheckBox();	required.setSelected	(this.forceGps);								//Boolean	forceGps;
		JTextField hint				= new JTextField(); hint.setText			(String.valueOf(this.hint));					//String	hint;
		JTextField adcChannel		= new JTextField(); adcChannel.setText		(String.valueOf(this.adcChannel));				//Integer	adcChannel;
		JTextField adcGain			= new JTextField(); adcGain.setText			(String.valueOf(this.adcGain));					//Integer	adcGain;
		JTextField analogueMultiply	= new JTextField(); analogueMultiply.setText(String.valueOf(this.analogueMultiply));		//Float	analogueMultiply;
		JTextField analogueAdd		= new JTextField(); analogueAdd.setText		(String.valueOf(this.analogueAdd));				//Float	analogueAdd;
		JCheckBox  plot				= new JCheckBox();	required.setSelected	(this.plot);									//Boolean	plot;
		JTextField height			= new JTextField(); height.setText			(String.valueOf(this.height));					//Integer	height;
		JTextField colour			= new JTextField(); colour.setText			(String.valueOf(this.colour));					//String	colour;
		JTextField minValue			= new JTextField(); minValue.setText		(String.valueOf(this.minValue));				//Float	minValue;
		JTextField maxValue			= new JTextField(); maxValue.setText		(String.valueOf(this.maxValue));				//Float	maxValue;
		JCheckBox  readOnly			= new JCheckBox();	required.setSelected	(this.readOnly);								//Boolean	readOnly;
		JTextField sendControlName	= new JTextField(); sendControlName.setText	(String.valueOf(this.sendControlName));			//String	sendControlName;
		JTextField controlOnSubform	= new JTextField(); controlOnSubform.setText(String.valueOf(this.controlOnSubform));		//String	controlOnSubform;
		JTextField getControlData	= new JTextField(); getControlData.setText	(String.valueOf(this.getControlData));			//String	getControlData;
		JTextField defaultValue		= new JTextField(); defaultValue.setText	(String.valueOf(this.defaultValue));			//String	defaultValue;
		
		editPanel.add(new JLabel("id"));				editPanel.add(id);
		editPanel.add(new JLabel("order"));				editPanel.add(order);
		editPanel.add(new JLabel("type"));				editPanel.add(type);
		editPanel.add(new JLabel("title"));				editPanel.add(title);
		editPanel.add(new JLabel("postTitle"));			editPanel.add(postTitle);
		editPanel.add(new JLabel("dbTitle"));			editPanel.add(dbTitle);
		editPanel.add(new JLabel("length"));			editPanel.add(length);
		editPanel.add(new JLabel("topic"));				editPanel.add(topic);
		editPanel.add(new JLabel("nmeaModule"));		editPanel.add(nmeaModule);
		editPanel.add(new JLabel("nmeaString"));		editPanel.add(nmeaString);
		editPanel.add(new JLabel("nmeaPosition"));		editPanel.add(nmeaPosition);
		editPanel.add(new JLabel("required"));			editPanel.add(required);
		editPanel.add(new JLabel("autoUpdate"));		editPanel.add(autoUpdate);
		editPanel.add(new JLabel("autoclear"));			editPanel.add(autoclear);
		editPanel.add(new JLabel("forceGps"));			editPanel.add(forceGps);
		editPanel.add(new JLabel("hint"));				editPanel.add(hint);
		editPanel.add(new JLabel("adcChannel"));		editPanel.add(adcChannel);
		editPanel.add(new JLabel("adcGain"));			editPanel.add(adcGain);
		editPanel.add(new JLabel("analogueMultiply"));	editPanel.add(analogueMultiply);
		editPanel.add(new JLabel("analogueAdd"));		editPanel.add(analogueAdd);
		editPanel.add(new JLabel("plot"));				editPanel.add(plot);
		editPanel.add(new JLabel("height"));			editPanel.add(height);
		editPanel.add(new JLabel("colour"));			editPanel.add(colour);
		editPanel.add(new JLabel("minValue"));			editPanel.add(minValue);
		editPanel.add(new JLabel("maxValue"));			editPanel.add(maxValue);
		editPanel.add(new JLabel("readOnly"));			editPanel.add(readOnly);
		editPanel.add(new JLabel("sendControlName"));	editPanel.add(sendControlName);
		editPanel.add(new JLabel("controlOnSubform"));	editPanel.add(controlOnSubform);
		editPanel.add(new JLabel("getControlData"));	editPanel.add(getControlData);
		editPanel.add(new JLabel("defaultValue"));		editPanel.add(defaultValue);
		
		
		
		return editPanel;
	}
	
	
	/**
	 * Read the data for a single from item (control or command) from the UDF table 
	 * definition. Store in local variables for use as required when forms are created.
	 */
	private void readTableDefRecord() {
		UDFTableDefinition udfTableDefinition = formDescription.getUdfTableDefinition();
		/*
		 * For most of the parameters, use getValue instead of functions like getIntegerValue().
		 * This is because getIntegerValue will convert null's to zeros and it's important that 
		 * the nulls remain nulls for correct control operation. 
		 * The exception is getStringValue since this will still return null if the value is
		 * null, but will trim non null strings - also very important ! 
		 */
		id				= (Integer) udfTableDefinition.getIndexItem()		.getValue();
		order			= (Integer) udfTableDefinition.getOrder()			.getValue();
		type			= udfTableDefinition.getType()						.getStringValue();
		title			= udfTableDefinition.getTitle()						.getStringValue();
		postTitle		= udfTableDefinition.getPostTitle()					.getStringValue();
		dbTitle			= udfTableDefinition.getDbTitle()					.getStringValue();
		length			= (Integer) udfTableDefinition.getLength()			.getValue();
		topic			= udfTableDefinition.getTopic()						.getStringValue();
		nmeaModule		= udfTableDefinition.getNmeaModule()				.getStringValue();	
		nmeaString		= udfTableDefinition.getNmeaString()				.getStringValue();
		nmeaPosition	= (Integer) udfTableDefinition.getNmeaPosition()	.getValue();
		required		= (Boolean) udfTableDefinition.getRequired()		.getValue();
		autoUpdate		= (Integer) udfTableDefinition.getAutoUpdate()		.getValue();
		autoclear		= (Boolean) udfTableDefinition.getAutoclear()		.getValue();
		forceGps		= (Boolean) udfTableDefinition.getForceGps()		.getValue();
		hint			=  udfTableDefinition.getHint()						.getStringValue();
		adcChannel		= (Integer) udfTableDefinition.getAdcChannel()		.getValue();
		adcGain			= (Integer) udfTableDefinition.getAdcGain()			.getValue();
		analogueMultiply= (Float) udfTableDefinition.getAnalogueMultiply()	.getValue();
		analogueAdd		= (Float) udfTableDefinition.getAnalogueAdd()		.getValue();
		plot			= (Boolean) udfTableDefinition.getPlot()			.getValue();
		height			= (Integer) udfTableDefinition.getHeight()			.getValue();
		colour			=  udfTableDefinition.getColour()					.getStringValue();
		minValue		= (Float) udfTableDefinition.getMinValue()			.getValue();
		maxValue		= (Float) udfTableDefinition.getMaxValue()			.getValue();
		readOnly		= (Boolean) udfTableDefinition.getReadOnly()		.getValue();
		sendControlName	= udfTableDefinition.getSendControlName()			.getStringValue();
		controlOnSubform= udfTableDefinition.getControlOnSubform()			.getStringValue();
		getControlData	= udfTableDefinition.getGetControlData()			.getStringValue();
		defaultValue	= udfTableDefinition.getDefaultValue()				.getStringValue();
	}
	

	
	

	/**
	 * @return the udfTableDefinition
	 */
	public UDFTableDefinition getUdfTableDefinition() {
		return formDescription.getUdfTableDefinition();
	}



	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}



	/**
	 * @return the order
	 */
	public Integer getOrder() {
		return order;
	}



	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}



	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}



	/**
	 * @return the postTitle
	 */
	public String getPostTitle() {
		return postTitle;
	}


	public int getNumDBColumns() {
		return 1;
	}

	/**
	 * Return the database title. It's perfectly acceptable for
	 * this not to have been filled in in which case
	 * return the main title of the control. If that is also null, 
	 * then it will cause an error.  
	 * @return the dbTitle
	 */
	public String getDbTitle() {//check duplicates somewhere
		if (dbTitle == null || dbTitle.length() == 0) {
			return EmptyTableDefinition.deblankString(title);
		}
		return dbTitle;
	}



	/**
	 * @return the length
	 */
	public Integer getLength() {
		return length;
	}



	/**
	 * @return the topic
	 */
	public String getTopic() {
		if (topic == null) {
			return title;
		}
		return topic;
	}



	/**
	 * @return the nmeaString
	 */
	public String getNmeaModule() {
		return nmeaModule;
	}


	/**
	 * @return the nmeaString
	 */
	public String getNmeaString() {
		return nmeaString;
	}



	/**
	 * @return the nmeaPosition
	 */
	public Integer getNmeaPosition() {
		return nmeaPosition;
	}



	/**
	 * @return the required
	 */
	public Boolean getRequired() {
		return required;
	}



	/**
	 * @return the autoUpdate
	 */
	public Integer getAutoUpdate() {
		return autoUpdate;
	}
	
	/**
	 * @param the autoUpdate
	 */
	public void setAutoUpdate(Integer autoUpdate) {
		this.autoUpdate=autoUpdate;
	}



	/**
	 * @return the autoclear
	 */
	public Boolean getAutoclear() {
		return autoclear;
	}



	/**
	 * @return the forceGps
	 */
	public Boolean getForceGps() {
		return forceGps;
	}



	/**
	 * @return the hint
	 */
	public String getHint() {
		return hint;
	}



	/**
	 * @return the adcChannel
	 */
	public Integer getAdcChannel() {
		return adcChannel;
	}



	/**
	 * @return the adcGain
	 */
	public Integer getAdcGain() {
		return adcGain;
	}



	/**
	 * @return the analogueMultiply
	 */
	public Float getAnalogueMultiply() {
		return analogueMultiply;
	}



	/**
	 * @return the analogueAdd
	 */
	public Float getAnalogueAdd() {
		return analogueAdd;
	}



	/**
	 * @return the plot
	 */
	public Boolean getPlot() {
		return plot;
	}



	/**
	 * @return the height
	 */
	public Integer getHeight() {
		return height;
	}



	/**
	 * @return the colour
	 */
	public String getColour() {
		return colour;
	}



	/**
	 * @return the minValue
	 */
	public Float getMinValue() {
		return minValue;
	}



	/**
	 * @return the maxValue
	 */
	public Float getMaxValue() {
		return maxValue;
	}



	/**
	 * @return the readOnly
	 */
	public Boolean getReadOnly() {
		return readOnly;
	}



	/**
	 * @return the sendControlName
	 */
	public String getSendControlName() {
		return sendControlName;
	}



	/**
	 * @return the controlOnSubform
	 */
	public String getControlOnSubform() {
		return controlOnSubform;
	}



	/**
	 * @return the getControlData
	 */
	public String getGetControlData() {
		return getControlData;
	}



	/**
	 * @return the defaultField
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(Integer length) {
		this.length = length;
	}

	/**
	 * @return the motherFormDescription
	 */
	public FormDescription getFormDescription() {
		return formDescription;
	}

	/**
	 * @return the itemErrors
	 */
	public int getItemErrors() {
		return itemErrors;
	}

	/**
	 * @param itemErrors the itemErrors to set
	 */
	public void setItemErrors(int itemErrors) {
		this.itemErrors = itemErrors;
	}
	
	/**
	 * Add one to the item error count. 
	 */
	public void addItemError() {
		this.itemErrors++;
	}
	
}
