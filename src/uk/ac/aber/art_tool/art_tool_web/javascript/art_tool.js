
/* Functions for Sapient */

//function to upload the selected file to the server, using XHR			
function uploadFile() {
	// name of the paper
	name = $F('papername');
	// allowable: letters, numbers, underscore, space
	pattern = /^(\w|_)+$/;
	// if the filename is allowable
	if (name.match(pattern)) {
		// DEBUG
		$('filecheck').innerHTML = name;
		if (!$('a' + name) && !$('li' + name)) {
			// gets the text of the file out of the file object
			// *NOTE*: only works in Firefox 3 currently
			file = $('paperfile').files.item(0).getAsText("");
			// resets the form so that the same paper could be
			// immediately uploaded with a different name
			$('paperupload').reset();
			// XHR action: to add the paper
			// papername contains the name of the file to be added
			// paperfile contains the contents of the file as text
			new Ajax.Request('ART', {
				method :'post',
				parameters : {
					action :'addpaper',
					papername :name,
					paperfile :file
				},
				// response goes to populateLinks()
				onComplete :populateLinks
			});
			// get here if name is already taken
		} else {
			alert('This name is already taken -- choose a different name, or click delete on the existing file');
		}
		// get here if the filename contained disallowed characters
	} else {
		alert('Please use only letters, numbers and underscores in filenames.')
		// DEBUG -- make this a more user friendly message
		// $('filecheck').innerHTML = name + ' was rejected';
		$('paperupload').reset();
	}
}

// function to delete a paper from the server.
// papername: the name of the paper to be deleted.
function deletePaper(papername) {
	// XHR action: to delete the paper
	// name: the name of the paper to be deleted
	new Ajax.Request('ART', {
		method :'post',
		parameters : {
			action :'deletebook',
			name :papername
		},
		// response goes to populateLinks()
		onComplete :populateLinks
	});
}

function chooseMode(id) {
	$('choosemode').innerHTML = '<h3>' + id + '</h3>';
	if (id.length != 0) {
		var chooselinkarray = $$('a.chooselink');
		chooselinkarray.each( function(link) {
			$('choosemode').innerHTML = link;
			link.show();
		});
		var showlinkarray = $$('a.showlink');
		showlinkarray.each( function(link) {
			link.remove();
		});
		$("a" + id).hide();
		$("li" + id)
				.insert(
						{
							top :'<a class="showlink" href=ART?action=showmode2&sid=s1&name='
									+ id
									+ '>Show Paper '
									+ id
									+ ' in mode 2</a>'
						});
		$("li" + id)
				.insert(
						{
							top :'<a class="showlink" href=ART?action=showmode1&sid=s1&name='
									+ id
									+ '>Show Paper '
									+ id
									+ ' in mode 1</a>'
						});
	}
}

// function to get the list of files from the server and pass them to
// populateLinks() for display
function getLinks(servertype) {
	// XHR action to get the filename list
	new Ajax.Request('ART?action=addlinks', {
		method :'get',
		parameters : {
			servertype :servertype
		},
		// response goes to populateLinks()
		onComplete :populateLinks
	});
}

// function to create and update the paper links at the top of the page
// response: The XHResponse from the server after another function
// was called, contains a string with a comma separated list of filenames
function populateLinks(response) {
	// the response text -- a comma separated list of filenames
	resp = response.responseText;
	// emptying the list
	$('paperlinks').innerHTML = '';
	// DEBUG
	$('response').innerHTML = resp;
	// separate by comma into array
	links = resp.split(",");
	// buttons must be in forms, so these surround the loop
	// goes through every string in the array (except the last one,
	// because there is one too many strings owing to the way the commas
	// were appended serverside) and creates a link to show the paper
	// and a button to delete the paper.

	for ( var i = 0; i < (links.length - 1); i++) {
		string = '<li id="li'
				+ links[i]
				+ '">'// <a class="chooselink" "id="a'
				// + links[i]
				// + '" href="ART" onclick="chooseMode(\''
				// + links[i]
				// + '\');return false;">'
				// + links[i]
				// + '</a>'
				+ '<a class="showlink" href=ART?action=showmode2&sid=s1&name='
				+ links[i]
				+ '>'
				+ links[i]
				+ '</a>'
				+ '<input class= "deletebutton" type="button" onclick="if(confirm(\'Are you sure you want to delete the paper?\'))deletePaper(\''
				+ links[i] + '\')" value="Delete"/></li>';
		// inserts each link at the bottom of the <ul>
		$('paperlinks').insert( {
			bottom :string
		});
	}
	// inserts the form tags.
	$('paperlinks').insert( {
		top :'<form>'
	});
	$('paperlinks').insert( {
		bottom :'</form>'
	});
}

/* functions for the show modes */
// Hash to contain the concept ids per sentence
var conceptHash = $H();
// var testvar = "I exist!";
var subTypeHash = $H();

/*
 * var colourHash = $H({ 'Bac': 666, 'Con': 600, 'Exa': 660, 'Exp': 060, 'Goa':
 * 066, 'Hyp': 006, 'Met': 606, 'Mod': c00, 'Mot': cc0, 'Obj': 0c0, 'Obs': 0cc,
 * 'Pro': 00c, 'Res': c0c });
 */
function initialise() {
	//alert('initialising');
	fillHashes();
	conceptHash.each( function(pair) {
		typedd = $(pair.key.toString()).down('select.type');
		type = typedd.getValue();
		colourDDs(typedd, type);
	});
	getComments();
}

function getComments() {
	title = $$('title').reduce().readAttribute('filename');
	new Ajax.Request('ART?action=getcomments&name=' + title, {
		method :'get',
		// response goes to updateComments()
		onComplete :updateComments
	});
}

function updateComments(response) {
	resp = response.responseText;
	if (resp != null && resp != "null") {
		$('comment').innerHTML = resp;
	} else {
		$('comment').innerHTML = "Enter comments here";
	}
}

//There may be a problem with this causing second saves to lose subtype data of advantages and disadvantages
function fillHashes() {
	//alert('fillHashes');
	subtypeddArray = $$('select.subtype');
	conceptiddArray = $$('select.conceptid');

	subtypeddArray.each( function(subtypedd) {
		value = subtypedd.getValue();
		// alert("subtype value is:"+value);
		if (value != "default") {
			subsid = parseInt(subtypedd.readAttribute("sid"));
			subTypeHash.set(subsid, value);
			if(subsid > 108) {
				//alert("sid:"+subsid + "value:" + value);
			}
		}
	});

	conceptiddArray.each( function(conceptidd) {
		value = conceptidd.getValue();
		if (value != "default") {
			consid = parseInt(conceptidd.readAttribute("sid"));
			conceptHash.set(consid, value);
			// alert("sid:"+consid + "value:" + value);
		}
	});
	fillConceptDropDowns();
	fillSubtypeDropDowns();
}

function fillConceptDropDowns() {
	//('fillConceptDropDowns');
	// needs amending so that fewer options are available
	conceptHash.each( function(pair) {
				var sid = pair.key;
				var conceptidd = $(sid.toString()).down('select.conceptid');
				var conceptidValue = pair.value;
				//REMOVING "None" as an option.
				//if (conceptidValue != "None") {
					var ctype = conceptidValue.substring(0, 3);
					var subHash = getTypeSubHash(ctype);
					var affectedDDs = getAffectedDDs(subHash);

					var currentDD = conceptidd;
					var currentIndex = affectedDDs.indexOf(currentDD);
					var currentSID = parseInt(conceptidd.readAttribute('sid'));
					var currentVal = currentDD.getValue();

					var nextDD = affectedDDs[currentIndex + 1];

					var previousDD = affectedDDs[currentIndex - 1];

					var optionsToAdd = '<option class="default" value="default">Select a concept id</option>';

					var uniqVals = $A();

					for ( var i = 0; i <= currentIndex; i++) {
						var val = affectedDDs[i].getValue();
						// if(val != currentVal){
						uniqVals.push(val);
						// }
					}
					uniqVals = uniqVals.uniq();
					uniqVals = uniqVals.sortBy( function(value) {
						return parseInt(value.substring(3));
					});
					var count = 0;
					uniqVals.each( function(val) {
						if (val == currentVal) {
							count++;
						}
					});
					currentValExists = false;
					if (count > 1) {
						currentValExists = true;
					}
					var alertString='';
					// alert('currentValExists? ' + currentValExists);
					uniqVals.each( function(value) {
						 alertString = alertString + ' ' + value;
							optionsToAdd = optionsToAdd + '<option class="'
									+ ctype + '" value="' + value + '">'
									+ value + '</option>';
					});
					//alert("The uniqVals are" + alertString);
					/*
					 * if(currentVal > previousVal) { optionsToAdd =
					 * optionsToAdd + '<option class="'+ctype+'"
					 * value="'+previousVal+'">'+previousVal+'</option>'; }
					 */
					// optionsToAdd = optionsToAdd + '<option class="'+ctype+'"
					// value="'+currentVal+'">'+currentVal+'</option>';
					if (currentDD != affectedDDs.last()) {
						// alert("nextDD" + nextDD + "sid" + sid);
						var nextVal = nextDD.getValue();
						if (currentDD != affectedDDs.first()) {

							previousVal = previousDD.getValue();

							if (currentVal == previousVal
									&& currentVal < nextVal) {
								optionsToAdd = optionsToAdd + '<option class="'
										+ ctype + '" value="' + nextVal + '">'
										+ nextVal + '</option>';
							}
						}
					} else if (currentValExists
							|| currentDD == affectedDDs.first()) {
						optionsToAdd = optionsToAdd + '<option class="new" value="new">Add new ID</option>';
					}
					// to store all the options for this dropdown
					var conceptoptions = conceptidd.descendants();
					conceptoptions.each( function(option) {
						// remove all options from the DOM
							option.remove();
						});
					conceptidd.insert( {
						bottom :optionsToAdd
					});
					// alert("added options");
					conceptidd.down('[value="' + currentVal + '"]').selected = true;
					//REMOVING "None" as an option.
				/*} else {
					conceptidd.innerHTML = '<option class="default" value="default"></option><option class="None" value="None">None</option>';
					conceptidd.down('[value="None"]').selected = true;
				}*/
			});

}

function fillSubtypeDropDowns() {
	//alert('fillSubtypeDropDowns');
	conceptHash.each( function(pair) {
		ctype = pair.value.substring(0, 3);
		var subKey = pair.key;
		if (subTypeHash.get(subKey)) {
			var subtypedd = $(subKey.toString()).down('select.subtype');
			var subValue = subTypeHash.get(subKey);
			// to store all the options for this dropdown
			var subtypeoptions = subtypedd.descendants();
			// for each of the options in the dropdown
			subtypeoptions.each( function(option) {
				// if it's not default, remove it from the DOM
					if (!option.hasClassName('default')) {
						option.remove();
					}
				});
			//If you have specific rules for subtypes, put them here
			if (ctype == "Met") {
				theArray = new Array("Old", "New", "Advantage", "Disadvantage");
			} else if (ctype == "Obj") {
				theArray = new Array("New", "Advantage", "Disadvantage");
			}
			var optionsToAdd = "";
			theArray.each( function(option) {
				optionsToAdd = optionsToAdd + '<option class="' + option
						+ '" value="' + option + '">' + option + '</option>';
			});
			subtypedd.insert( {
				bottom :optionsToAdd
			});
			// alert("added options");
			subtypedd.down('[value="' + subValue + '"]').selected = true;
		} else {
			var subtypedd = $(subKey.toString()).down('select.subtype');
			subtypedd.innerHTML = '<option class="default" value="default"><option class="None" value="None">None</option>';
			subtypedd.down('[value="None"]').selected = true;
		}
	});
}
//Make your own set of colour rules for your concepts here by changing the case names to fit your annotation schema specified in the xsl
function colourDDs(typedd, type) {
	// alert('type: '+ type);
	subtypedd = Element.next(typedd, 'select.subtype');
	conceptidd = Element.next(typedd, 'select.conceptid');
	switch (type) {
	case "Bac":
		typedd.setStyle( {
			backgroundColor :"#666",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#666",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#666",
			color :"#FFF"
		});
		break;
	case "Con":
		typedd.setStyle( {
			backgroundColor :"#600",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#600",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#600",
			color :"#FFF"
		});
		break;
	case "Exa":
		typedd.setStyle( {
			backgroundColor :"#660",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#660",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#660",
			color :"#FFF"
		});
		break;
	case "Exp":
		typedd.setStyle( {
			backgroundColor :"#060",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#060",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#060",
			color :"#FFF"
		});
		break;
	case "Goa":
		typedd.setStyle( {
			backgroundColor :"#066",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#066",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#066",
			color :"#FFF"
		});
		break;
	case "Hyp":
		typedd.setStyle( {
			backgroundColor :"#006",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#006",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#006",
			color :"#FFF"
		});
		break;
	case "Met":
		typedd.setStyle( {
			backgroundColor :"#606",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#606",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#606",
			color :"#FFF"
		});
		break;
	case "Mod":
		typedd.setStyle( {
			backgroundColor :"#c00",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#c00",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#c00",
			color :"#FFF"
		});
		break;
	case "Mot":
		typedd.setStyle( {
			backgroundColor :"#cc0",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#cc0",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#cc0",
			color :"#FFF"
		});
		break;
	case "Obj":
		typedd.setStyle( {
			backgroundColor :"#0c0",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#0c0",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#0c0",
			color :"#FFF"
		});
		break;
	case "Obs":
		typedd.setStyle( {
			backgroundColor :"#0cc",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#0cc",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#0cc",
			color :"#FFF"
		});
		break;
	case "Pro":
		typedd.setStyle( {
			backgroundColor :"#00c",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#00c",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#00c",
			color :"#FFF"
		});
		break;
	case "Res":
		typedd.setStyle( {
			backgroundColor :"#c0c",
			color :"#FFF"
		});
		subtypedd.setStyle( {
			backgroundColor :"#c0c",
			color :"#FFF"
		});
		conceptidd.setStyle( {
			backgroundColor :"#c0c",
			color :"#FFF"
		});
		break;

	default:
		typedd.setStyle( {
			backgroundColor :"#FFF",
			color :"#000"
		});
		subtypedd.setStyle( {
			backgroundColor :"#FFF",
			color :"#000"
		});
		conceptidd.setStyle( {
			backgroundColor :"#FFF",
			color :"#000"
		});
	}
}

// This is called whenever a 'type' drop-down is changed
function changeType(typedd, conceptIDsUsed) {
	//alert('in changeType -- conceptIDsUsed is '+ conceptIDsUsed);
	// Find out which sentence we're looking at
	var sid = parseInt(typedd.readAttribute("sid"));
	// remove any entry for this sentence from the hashes
	removeFromHash(sid);
	subTypeHash.unset(sid);
	var typeSelected = typedd.getValue();
	colourDDs(typedd, typeSelected);
	// alert("type selected:" + typeSelected);
	//REMOVING "None" as an option.
	// if they haven't selected "None" for the type
	/*typeSelected != "None" && */
	if (typeSelected != "default") {
		getsubtypeOptions(typedd, conceptIDsUsed);
		//REMOVING "None" as an option.
		// if it is None, empty the subtype menu.
	//} else if (typeSelected == "None") {
		//conceptHash.set(sid, "None");
		//typedd.next('select.subtype').innerHTML = '<option sid="' + sid
			//	+ '" class="default" value="default"><option sid="' + sid
				//+ '" class="None" value="None">None</option>';
	//	typedd.next('select.conceptid').innerHTML = '<option sid="' + sid
		//		+ '" class="default" value="default"></option><option sid="'
			//	+ sid + '" class="None" value="None">None</option>';
		//typedd.next('select.conceptid').down('[value="None"]').selected = true;
		//typedd.next('select.subtype').down('[value="None"]').selected = true;
	} else {
		typedd.next('select.subtype').innerHTML = '<option sid="' + sid + '" class="default" value="default"></option>';
		typedd.next('select.conceptid').innerHTML = '<option sid="' + sid + '" class="default" value="default"></option>';
		// leave this alert uncommented -- it is for the user
		notify('noan', sid);

	}
}

// This is called whenever a 'subtype' drop-down is changed
function changeSubType(typedd) {
	// Find out which sentence we're looking at
	sid = parseInt(typedd.readAttribute("sid"));
	subTypeVal = typedd.getValue();
	if (subTypeVal == "None") {
		subTypeHash.unset(sid);
	} else {
		subTypeHash.set(sid, subTypeVal);
	}
}

// this removes an entry from the hash and then checks to see if any 'rejiggery'
// is necessary
function removeFromHash(sid) {
	// simultaneously checks for this sentence in the hash and assigns the
	// concept if present
	if (concept = conceptHash.get(sid)) {
		// alert('found this sentence in the hash');
		conceptHash.unset(sid);

		// alert('values are '+conceptHash.values());
		// if there is now nothing with that exact concept id in the hash
		if (conceptHash.values().indexOf(concept) == -1) {
			// rejiggering of the numbers in the hash is required
			fixHash(concept, sid);
		}
	}
}

// this function fixes the hash contents, reducing the affected entries
// numerically by one.
// It then calls fixDropDowns to update all the affected sentences drop-downs.

// concept is the previously selected concept id that is the last one of its
// type and has been removed
function fixHash(concept, sid) {
	// alert('fixHash');
	var ctype = concept.substring(0, 3);
	// the previously selected number
	var number = parseInt(concept.substring(3));
	// to store a subHash of the affected sentences
	affectedEntries = $H();
	// to store a list of affected drop-downs
	// affectedDDs = $A();
	conceptArray = $A();

	// for each entry in the main hash
	conceptHash.each( function(pair) {
		// alert('key: ' + pair.key + 'value: ' + pair.value);
			thisNum = parseInt(pair.value.substring(3));
			thisType = pair.value.substring(0, 3);
			// if it is of the affected type
			if (thisType == ctype) {
				// alert('sentenceid='+pair.key);
				// add the sentence's drop-down to the array (nothing happens to
				// this yet -- it is simply passed on to fixDropDowns())
				// affectedDDs.push($(pair.key).down('select.conceptid'));
				// and if it has a numerical value greater than the one which
				// was removed
				if (thisNum > number) {
					// add it to the new hash to be rejigged
					affectedEntries.set(pair.key, pair.value);
				}
			}
		});

	/*
	 * affectedDDs.each(function(dd){ alert('affected
	 * drop-down:'+dd.readAttribute('sid')); });
	 */

	// for each entry in the new hash
	affectedEntries.each( function(pair) {
		// store its number
			var num = parseInt(pair.value.substring(3));
			// make a new number for it
			newNumber = num - 1;
			// set it in the main hash to its new number
			conceptHash.set(pair.key, (pair.value.substring(0, 3) + newNumber));
		});
	conceptHash.each( function(pair) {
		// alert('pair:'+pair.key+':'+pair.value);
		});

	conceptHash.each( function(pair) {
		// if it is of the affected type
			if (pair.value.substring(0, 3) == ctype) {
				conceptArray.push(pair.value);
			}
		});

	uniqueConceptArray = conceptArray.uniq();
	uniqueConceptArray.each( function(concept) {
		// alert('concept:'+concept);
		});
	// finally, fix the drop-downs for this concept type
	fixDropDowns(number, ctype, uniqueConceptArray);
}

// all the dropdowns need fixing too. Number is the number that was removed from
// the hash.
// affectedDDs is a list of the dropdowns that need refilling.
function fixDropDowns(number, ctype, uniqueConceptArray) {
	// alert('fixDropDowns');
	// needs amending so that fewer options are available
	// var optionsToAdd = "";
	/*
	 * uniqueConceptArray.each(function(concept){ //insert a new option in the
	 * drop-down for this concept //alert('value=' + concept); optionsToAdd =
	 * optionsToAdd + '<option class="' + concept.substring(0,3) + '" value="' +
	 * concept + '">' + concept + '</option>'; });
	 */
	/*
	 * affectedDDs.each(function(dd){ alert('unsorted dd:'+
	 * dd.readAttribute('sid')); })
	 */
	var subHash = getTypeSubHash(ctype);
	var affectedDDs = getAffectedDDs(subHash);

	// for all the affected drop-downs, that is, drop-downs of the same type
	affectedDDs.each( function(dd) {

		var sid = parseInt(dd.readAttribute('sid'));
		// alert('sid: '+sid);
			var chosenid = dd.getValue();
			var num = parseInt(chosenid.substring(3));
			// alert('num='+num + ',number='+number);
			// if the selected value is one of those affected in the hash
			if (num > number) {
				// reduce its number by one so that it can be properly set again
				// later
				num = num - 1;
				chosenid = ctype + (num);

				// alert('math done on chosenid:'+chosenid);
				// alert('the index of dd is ' + affectedDDs.indexOf(dd));
			}

			// to store all the options for this dropdown
			var conceptoptions = dd.descendants();

			// for each of the options in the dropdown
			conceptoptions.each( function(option) {
				// remove it from the DOM
					option.remove();
					// alert('The option to be removed is ' + option);

				});

			var currentIndex = affectedDDs.indexOf(dd);
			var previousIndex = currentIndex - 1;
			var nextIndex = currentIndex + 1;
			var currentVal = chosenid;
			var currentID = chosenid.substring(3);
			var uniqVals = $A();
			var currentOption = '<option class="' + ctype + '" value="'
					+ currentVal + '">' + currentVal + '</option>';
			// alert("currentIndex: " + currentIndex + ", currentVal: " +
			// currentVal);
			var optionsToAdd = '';

			if (dd == affectedDDs.first()) {
				// don't mess with those numbers, previous is null!
				optionsToAdd = currentOption;
			} else {
				for ( var i = 0; i < currentIndex; i++) {
					// this time we must use the value from the hash
					var val = conceptHash.get(parseInt(affectedDDs[i]
							.readAttribute('sid')));
					uniqVals.push(val);
				}
				uniqVals = uniqVals.uniq();
				uniqVals = uniqVals.sortBy( function(value) {
					return parseInt(value.substring(3));
				});
				// var alertString = 'uniqVals: ';
				// uniqVals.each(function(val){
				// alertString = alertString + val + ', ';
				// });
				// alert(alertString);
				currentValExists = uniqVals.any( function(val) {
					return val == currentVal;
				});
				// alert('currentValExists? ' + currentValExists)
				uniqVals.each( function(value) {
					if (value != currentVal) {
						optionsToAdd = optionsToAdd + '<option class="' + ctype
								+ '" value="' + value + '">' + value
								+ '</option>';
					}
				});
				optionsToAdd = optionsToAdd + currentOption;
				var previousID = currentID - 1;
				var previousDD = affectedDDs[previousIndex];
				// var previousVal = previousDD.getValue();
				// cannot rely on drop-down content at this stage - must check
				// the hash
				var prevSid = parseInt(previousDD.readAttribute('sid'));
				var previousVal = conceptHash.get(prevSid);
				// alert("previousID: " + previousID + ", previousVal: " +
				// previousVal);
				// alert("dd: " + dd.readAttribute('sid') + "affectedDDs.last: "
				// + affectedDDs.last().readAttribute('sid'));
				// if(dd == affectedDDs.last()) {
				// alert('this is the last one of this type: ' + sid);
				/*
				 * if(currentVal > previousVal) { optionsToAdd = optionsToAdd + '<option
				 * class="' + ctype + '" value="' + previousVal + '">' +
				 * previousVal + '</option>'; }
				 */
				if (dd != affectedDDs.last()) {
					var nextID = currentID + 1;
					var nextDD = affectedDDs[nextIndex];
					// var nextVal = nextDD.getValue();
					// cannot rely on drop-down content at this stage - must
					// check the hash
					var nextSid = parseInt(nextDD.readAttribute('sid'));
					var nextVal = conceptHash.get(nextSid);

					// alert("nextID: " + nextID + ", nextVal: " + nextVal);

					// if current has same value as previous and next
					// if(currentVal == previousVal && currentVal == nextVal) {
					// only has self value, so add nothing

					// if current = previous, but current < next
					/* }else */
					if (currentVal == previousVal && currentVal < nextVal) {
						// has self value and next value
						optionsToAdd = optionsToAdd + '<option class="' + ctype
								+ '" value="' + nextVal + '">' + nextVal
								+ '</option>';
						// if current > previous
					}// else if(currentVal > previousVal) {
					// has self value and previous value
					// optionsToAdd = optionsToAdd + '<option class="' + ctype +
					// '" value="' + previousVal + '">' + previousVal +
					// '</option>';
					// }

				}
			}
			dd.insert( {
				bottom :optionsToAdd
			});
			// alert('chosenid:'+chosenid);
			// check for current val existing in the previous unique array of
			// all previous values that have been selected
			if (dd == affectedDDs.last() && dd != affectedDDs.first()
					&& currentValExists) {
				dd
						.insert( {
							bottom :'<option class="new" value="new">Add new ID</option>'
						});
			}
			dd.down('[value="' + currentVal + '"]').selected = true;
		});

}

// Function to populate the subtype drop-down when a type is selected
function getsubtypeOptions(typedd, conceptIDsUsed) {
	//alert('conceptIDsUsed: ' + conceptIDsUsed);
	var sid = parseInt(typedd.readAttribute('sid'));
	// get the relevant subtypes menu
	subtypedd = Element.next(typedd, 'select.subtype');
	subtypeoptions = subtypedd.descendants();
	// remove all options except for "None"
	subtypeoptions.each( function(option) {
		if (option.hasClassName("default")) {
			option.innerHTML = "Select a subtype";
		} else if (!option.hasClassName("None")) {
			option.remove();
		}
	});
	// Find out which type is selected
	ctype = typedd.getValue();
	// if it's one of the ones that needs stuff added, do so
	if (ctype == "Met") {
		subtypedd.innerHTML = '<option value="default" class="default">Select a subtype</option>';
		subtypedd.insert( {
			bottom :'<option value="Old">Old</option>'
		});
		subtypedd.insert( {
			bottom :'<option value="New">New</option>'
		});
		subtypedd.insert( {
			bottom :'<option value="Advantage">Advantage</option>'
		});
		subtypedd.insert( {
			bottom :'<option value="Disadvantage">Disadvantage</option>'
		});
		// make sure "default" is the selected subtype
		subtypedd.down('[value="default"]').selected = true;
	} else if (ctype == "Obj") {
		subtypedd.innerHTML = '<option value="default" class="default">Select a subtype</option>';
		subtypedd.insert( {
			bottom :'<option value="New">New</option>'
		});
		subtypedd.insert( {
			bottom :'<option value="Advantage">Advantage</option>'
		});
		subtypedd.insert( {
			bottom :'<option value="Disadvantage">Disadvantage</option>'
		});
		// make sure "default" is the selected subtype
		subtypedd.down('[value="default"]').selected = true;
	} else {
		// otherwise, just add "None" as the only option
		subtypedd.innerHTML = '<option class="None" value="None">None</option>';
	}
	if(conceptIDsUsed == true) {
	// Find the relevant conceptid drop-down
	conceptidd = Element.next(subtypedd, 'select.conceptid');
	//alert('using conceptids');
	// call the method to insert the options the first time
	getIdOptions(ctype, conceptidd);
	} else {
		//alert('sid: '+ sid + 'ctype: ' + ctype);
		conceptHash.set(sid,ctype);		
	}
}

function getTypeSubHash(ctype) {
	subHash = $H();
	conceptHash.each( function(pair) {
		// if it matches the current concept
			if (pair.value.substring(0, 3) == ctype) {
				// add it to the subhash
				subHash.set(pair.key, pair.value);
			}
		});
	return subHash;
}
function getAffectedDDs(subHash) {
	var affectedDDs = $A();
	subHash.each( function(pair) {
		affectedDDs.push($(pair.key.toString()).down('select.conceptid'));
	});
	affectedDDs = affectedDDs.sortBy( function(dd) {
		return parseInt(dd.readAttribute('sid'));
	});
	affectedDDs.each( function(dd) {
		// alert('order in getAffectedDDs: ' + dd.readAttribute('sid'));
		});
	return affectedDDs;
}

// fuction called the first time an id-drop-down needs filling
function getIdOptions(ctype, conceptidd) {
	// needs amending so that fewer options are available
	var sid = parseInt(conceptidd.readAttribute('sid'));
	var conceptoptions = conceptidd.descendants();
	// remove all options except for "default"
	conceptoptions.each( function(option) {
		if (option.hasClassName("default")) {
			option.innerHTML = "Select a concept id";
		} else {
			option.remove();
		}
	});
	var subHash = getTypeSubHash(ctype);

	var uniqVals = $A();
	// var sidInt= parseInt(sid); // sid is a string
	subHash.each( function(pair) {
		var key = pair.key;
		// alert("the key is " + key + "the value in the subhash is : " +
		// subHash.get(key));
			if (key < sid) {
				// alert("This value will appear in conceptIdDD: " +
				// subHash.get(key));
				uniqVals.push(subHash.get(key));
			}
		});
	
	uniqVals = uniqVals.uniq();
	uniqVals = uniqVals.sortBy( function(value) {
		return parseInt(value.substring(3));
	});
	uniqVals.each( function(val) {
		conceptidd.insert( {
			bottom :'<option class="' + ctype + '" value="' + val + '">' + val
					+ '</option>'
		});
	});
	// in any case, add the 'add new' option

	var affectedDDs = getAffectedDDs(subHash);
	// remember the current drop-down is not yet in the hash, because no id has
	// been chosen
	// if this ID is the only one of its type, or if it has the highest sid of
	// its type
	/*
	 * if((affectedDDs.size() == 0 || sidInt >
	 * affectedDDs.last().readAttribute('sid'))) { conceptidd.insert({bottom: '<option
	 * class="new" value="new">Add new ID</option>'}); if(affectedDDs.size() !=
	 * 0){ oldAddNewOption = affectedDDs.last().down('[value="new"]');
	 * if(oldAddNewOption) { oldAddNewOption.remove(); } } }
	 */
	if (!conceptidd.down('[value="new"]')) {
		conceptidd.insert( {
			bottom :'<option class="new" value="new">Add new ID</option>'
		});
	}
	if (!conceptidd.down('[value="default"]')) {
		conceptidd
				.insert( {
					top :'<option class="default" value="default">Select a concept id</option>'
				});
	}
	conceptidd.down('[value="default"]').selected = true;

	/*
	 * tempSID = 0; //find the highest numbered sentence
	 * subHash.each(function(pair){ if(pair.key > tempSID) { tempSID = pair.key; }
	 * }); if(tempSID != 0) { //and get its value var highestConceptID =
	 * subHash.get(tempSID);
	 * 
	 * //insert a new option in the drop-down for this concept
	 * conceptidd.insert({bottom: '<option class="'+ ctype + '" value="' +
	 * highestConceptID + '">' + highestConceptID + '</option>'});
	 * //alert(conceptidd.descendants()); }
	 */
}

/* called when something in the conceptid dropdown is selected */
function selectConceptID(conceptidd) {
	 //alert('selectConceptID');
	var chosenid = conceptidd.getValue();
	var sid = parseInt(conceptidd.readAttribute('sid'));
	var typedd = Element.previous(conceptidd, 'select.type');
	// var concept = typedd.getValue();
	var ctype = typedd.getValue();
	var uniqVals = $A();

	// If 'Add new' has been selected
	if (chosenid == 'new') {
		// alert('add new selected');
		removeFromHash(sid);
		conceptidd.down('[value="new"]').remove();
		addConceptID(sid, ctype);
	} else { // chosen id is not new
		// select an existing ID which is the first entry for this sentence, so
		// it
		// can't cause a conflict
		if (!conceptHash.get(sid)) {
			conceptHash.set(sid, chosenid);
		} else { // this sentence is already in the hash
			// remove whatever was selected before (if it's in the hash)
			var previouslySelectedOption = conceptHash.get(sid);
			if (previouslySelectedOption != null) { // there was a previously
													// selected option
				var subHash = getTypeSubHash(ctype);
				// alert('found this sentence in the hash');
				conceptHash.unset(sid);
				if (chosenid != "default") {
					conceptHash.set(sid, chosenid);
				}
				// if there is now nothing with that exact concept id in the
				// hash
				if (conceptHash.values().indexOf(previouslySelectedOption) == -1) {

					// rejiggering of the numbers in the hash is required
					fixHash(previouslySelectedOption, sid);
				} else { // no rejiggering is required
					// alert('no rejig required');
					var affectedDDs = getAffectedDDs(subHash);
					var currentDD = conceptidd;
					var currentIndex = affectedDDs.indexOf(currentDD);
					var currentSID = parseInt(conceptidd.readAttribute('sid'));
					// alert('current sid: ' + currentSID);
					var currentVal = currentDD.getValue();
					for ( var i = 0; i < currentIndex; i++) {
						var val = affectedDDs[i].getValue();
						if (val != currentVal) {
							uniqVals.push(val);
						}
					}
					uniqVals = uniqVals.uniq();
					uniqVals = uniqVals.sortBy( function(value) {
						return parseInt(value.substring(3));
					});
					var nextDD = null;
					var previousDD = null;
					if (currentDD != affectedDDs.first()) {
						previousDD = affectedDDs[currentIndex - 1];

					}
					if (currentDD != affectedDDs.last()) {
						nextDD = affectedDDs[currentIndex + 1];

					}
					var prevExists = uniqVals.any( function(val) {
						return val == previouslySelectedOption;
					});
					if (previousDD && previousDD != affectedDDs.first()) {
						// alert('previousDD exists, removing '+
						// previouslySelectedOption);
						if (!prevExists) {
							oldOption = previousDD
									.down('[value="' + previouslySelectedOption + '"]');
							if (oldOption) {
								oldOption.remove();
							}
						}
					} else if (previousDD == affectedDDs.first() && !prevExists) {
						currentDD.down(
								'[value="' + previouslySelectedOption + '"]')
								.remove();
					}
					var theDD = null;
					var count = 0;
					// go through all dropdowns following the current one
					for ( var i = (currentIndex + 1); i < affectedDDs.size(); i++) {
						// if they share the same value as the current dd's
						// value
						if (currentVal == affectedDDs[i].getValue()) {
							// make theDD point to this dd
							theDD = affectedDDs[i];
							// increment the count
							count++;
						}
					}
					// if there was exactly one future dd with the same value as
					// the current value
					if (count == 1) {
						// var theDDnextIndex = affectedDDs.indexOf(theDD) + 1;
						if (theDD && theDD != affectedDDs.last()) {
							var theDDnext = affectedDDs[affectedDDs
									.indexOf(theDD) + 1];
							var theDDnextVal = theDDnext.getValue();
							var selectedVal = theDD.getValue();
							// if the dd following THAT dd
							if (theDDnextVal > selectedVal) {
								theDD.insert( {
									bottom :'<option class="' + ctype
											+ '" value="' + theDDnextVal + '">'
											+ theDDnextVal + '</option>'
								});
								theDD.down('[value="' + selectedVal + '"]').selected = true;
							}
						}
					}

					// find the final one of this type
					lastDD = affectedDDs.last();
					// alert('chosen value: ' + chosenid);
					// alert('last value: ' + lastDD.getValue());
					// if it's got the same conceptid as this one
					// alert("the concept is: " + concept + "and the last
					// concept of this type is: " + conceptHash.get(lastSID));
					if (chosenid == lastDD.getValue()) {
						// alert('yes, we have definitely got to this point');
						// if the last one doesn't already have Add New as an
						// option
						// lastConDD = $(lastSID).down('select.conceptid');
						if (!lastDD.down('[value="new"]')) {
							// alert('it has no add new');
							var selected = lastDD.getValue();
							// insert add new as an option
							lastDD
									.insert( {
										bottom :'<option class="new" value="new">Add new ID</option>'
									});
							lastDD.down('[value="' + selected + '"]').selected = true;
						}
					} else {
						var count = 0;
						// find any dds with the same value as the last one
						affectedDDs.each( function(dd) {
							if (dd.getValue() == lastDD.getValue()) {
								count++;
							}
						});
						// when count is 1, the last value is unique, so if it
						// has 'add new', 'add new' should be removed.
						if (count == 1 && lastDD.down('[value="new"]')) {
							lastDD.down('[value="new"]').remove();
						}
					}// was not last dd of this type
				}// no rejiggering is required
			}// there was a previously selected option
		}// this sentence was already in the hash
	} // chosen id was not new
}// End of Function

function addConceptID(sid, ctype) {
	//alert('addConceptID');
	//prevCon = concept
	// should overwrite anything with the same key
	// if it's empty, add the first ID for this type with no. 1
	if (conceptHash.keys().size() == 0) {
		conceptHash.set(sid, ctype + '1');
		latestConcept = ctype + '1';
	} else {
		// var count = 1;
		subHash = getTypeSubHash(ctype);
		precedingArr = $A();
		subHash.each( function(pair) {
			if (pair.key < sid) {
				precedingArr.push(pair.value);
			}
		});

		var vals = precedingArr.uniq();
		var num = vals.size() + 1;
		// var conceptArr = precedingHash.values().uniq();
		// go through the concepts in the hash
		// conceptArr.each(function(concept) {
		// count++;
		// });
		// define the concept to be added to the hash
		latestConcept = ctype + num;
		//alert("latest concept: " + latestConcept);
		// add it to the hash
		conceptHash.set(sid, latestConcept);
	}
	updateHash(latestConcept, sid);
	// update all the drop-downs that are affected by this addition
	updateConceptIDs(sid, latestConcept, ctype);
}

function updateHash(latestConcept, sid) {
	//alert('updateHash');
	ctype = latestConcept.substring(0, 3);
	latestNum = parseInt(latestConcept.substring(3));
	subHash = getTypeSubHash(ctype);
	subHash.each( function(pair) {
		var num = parseInt(pair.value.substring(3));
		if (num >= latestNum && sid != pair.key) {
			num++;
			var newConceptID = ctype + num;
			conceptHash.set(pair.key, newConceptID);
			// alert("sentence " + pair.key + " now has
			// value:"+conceptHash.get(pair.key));
		}
	});
}
// CHANGE THIS
// After adding a new concept id, this updates any other dds that need to now
// offer it as an option
function updateConceptIDs(sid, latestConcept, ctype) {
	//alert('updateConceptIDs');
	// subHash containing only sids of this type
	var subHash = getTypeSubHash(ctype);
	var affectedDDs = getAffectedDDs(subHash);
	var currentDD = $(sid.toString()).down('select.conceptid');
	var currentIndex = affectedDDs.indexOf(currentDD);
	//var currentVal = currentDD.getValue();
	//alert('currentVal: ' + currentVal);
	//var currentNum = parseInt(currentVal.substring(3));
	// all the DDs with the same concept type
	if (currentIndex > 0) {
		var previousDD = affectedDDs[currentIndex - 1];
		var previousVal = previousDD.getValue();
	}
	if (currentIndex > 1) {
		var prevButOneDD = affectedDDs[currentIndex - 2];
		var prevButOneVal = prevButOneDD.getValue();
	}
	/*
	 * if(affectedDDs.size() > 1) { previous =
	 * affectedDDs[affectedDDs.size()-2]; previousVal = previous.getValue(); if
	 * (affectedDDs.size() > 2) { prevButOne =
	 * affectedDDs[affectedDDs.size()-3]; prevButOneVal = prevButOne.getValue(); } }
	 */
	// find the highest numbered sentence
	/*
	 * subHash.each(function(pair){ var key = pair.key; if((key > tempSID) &&
	 * (key != sid)) { lastbutone = tempSID; tempSID = key; } });
	 */
	if (previousDD && prevButOneDD) {
		//alert('at least 3rd of this type');
		// if the previous id is the same as the id before it
		if (previousVal == prevButOneVal) {
			//alert('prev 2 are the same');
			// var conceptidd = $(tempSID).down('select.conceptid');
			var selectedValue = previousVal;
			// insert the recently added concept into the previous dd as an
			// option
			previousDD.insert( {
				bottom :'<option class="' + ctype + '" value="' + latestConcept
						+ '">' + latestConcept + '</option>'
			});
			// remove add new from the previous drop down
			if (previousDD.down('[value="new"]')) {
				//alert('removing "Add New"');
				previousDD.down('[value="new"]').remove();
			}
			previousDD.down('[value="' + selectedValue + '"]').selected = true;
		}
		// add latest concept to the current idd
		currentDD.insert( {
			bottom :'<option class="' + ctype + '" value="' + latestConcept
					+ '">' + latestConcept + '</option>'
		})
		// if it is not the first one of its type
	} else if (previousDD) {
		//alert('2nd of type');
		 var val = subHash.get(parseInt(previousDD.readAttribute('sid')));
		// var conceptidd = $(tempSID).down('select.conceptid');
		var hasOneAlready = previousDD.down('[value="' + latestConcept + '"]');
		// remove add new from the current drop down
		if (previousDD.down('[value="new"]')) {
			//alert('removing "Add New"');
			previousDD.down('[value="new"]').remove();
		}
		// add previous value to the current dd if it is not already there
		if (!hasOneAlready) {
			currentDD.insert( {
				bottom :'<option class="' + ctype + '" value="' + val + '">'
						+ val + '</option>'
			})
		}
		// add latest concept to the current idd
		currentDD.insert( {
			bottom :'<option class="' + ctype + '" value="' + latestConcept
					+ '">' + latestConcept + '</option>'
		})
	} else {
		//alert('1st of its type');
		// add latest concept to the current idd
		currentDD.insert( {
			bottom :'<option class="' + ctype + '" value="' + latestConcept
					+ '">' + latestConcept + '</option>'
		})
	}
	// looping over the subhash to update any values higher than or equal to the
	// current one
	for ( var i = currentIndex + 1; i < affectedDDs.size(); i++) {
		thisSid = parseInt(affectedDDs[i].readAttribute('sid'));
		optionsToAdd = '<option class="default" value="default">Select a concept id</option>';
		valueArray = $A();
		subHash.each( function(pair) {
			if (pair.key <= thisSid) {
				valueArray.push(pair.value);
			}
		});
		uniqArray = valueArray.uniq();
		uniqArray = uniqArray.sortBy( function(value) {
			return parseInt(value.substring(3));
		});

		uniqArray.each( function(value) {
			optionsToAdd = optionsToAdd + '<option class="' + ctype
					+ '" value="' + value + '">' + value + '</option>';
		});
		affectedDDs[i].innerHTML = optionsToAdd;
		affectedDDs[i].down('[value="' + subHash.get(thisSid) + '"]').selected = true;
	}
	;

}

/* function for saving the editted paper in mode2.xsl */

// use an XMLHTTPRequest to send the whole HTML page to the server to be
// converted
function savePaper() {
	// get the paper with its annotations from the div
	// alert($('mainpage').down(1));
	// pass on the comments info
	comments = $('comment').getValue();
	title = $$('title').reduce().readAttribute('filename');
	// alert(titleElem.readAttribute('filename'));
	// objJSON={
	// "conceptHash": conceptHash
	// };
	// var strJSON = encodeURIComponent(JSON.stringify(objJSON));
	//alert('109: ' + subTypeHash.get(109));
	//alert('113: ' + subTypeHash.get(113));
	var conceptJSON = Object.toJSON(conceptHash);
	var subtypeJSON = Object.toJSON(subTypeHash);
	// alert("The strJSON is " + strJSON);

	new Ajax.Request('ART?action=savePaperMode2', {
		method :'post',
		parameters : {
			name :title,
			comment :comments,
			conceptJSON :conceptJSON,
			subtypeJSON :subtypeJSON
		},
		// response goes to
		onComplete :notify('save', null)
	});

}

function notify(string, sid) {
	// leave this alert uncommented -- it is for the user
	if (string == 'save') {
		alert("The changes have been saved");
	} else if (string == 'noan') {
		alert('Sentence ' + sid + ' has not been annotated');
	}
}

function clearARTAnnotations() {
	var really = confirm("Are you sure you want to remove all your annotations? This operation cannot be undone.");
	if (really) {
		title = $$('title').reduce().readAttribute('filename');
		new Ajax.Request('ART?action=clearARTAnnotations', {
			method :'post',
			parameters : {
				name :title
			},
			onComplete :setTimeout("location.reload(true)", 2000)
		});
	}
}

function showMode2(response) {
	new Ajax.Request('ART?action=showMode2', {
		method :'get',
		parameters : {
			name :response.responseText,
			mode :2
		}

	});
}

function toggleOscarKey(thevalue) {
	showButton = '<input id="toggle" type="button" value="Show" onclick="toggleOscarKey(\'show\');"/>';
	hideButton = '<input id="toggle" type="button" value="Hide" onclick="toggleOscarKey(\'hide\');"/>';
	if (thevalue == "hide") {
		$('oscarlist').hide();
		$('toggle').replace(showButton);
	} else {
		$('oscarlist').show();
		$('toggle').replace(hideButton);
	}
}
