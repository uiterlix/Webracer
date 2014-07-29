;Quintus.Websocket = function (Q) {

    // prepare websocket connection

    var connection;

    var connect = function() {

        var url = "ws://" + window.location.host + "/ws/";

        connection = new WebSocket(url);

        connection.onopen = function(){
            console.log("webSocket open");
            registerClient();
        }

        connection.onerror = function (error) {
            console.log("webSocket error: " + error);
            // TODO handle error
        };

        connection.onmessage = function (message) {
            console.log("received message: " + message.data);

            var jsonMessage = JSON.parse(message.data);
            var command = 'ws-' + jsonMessage.command;
            var data = jsonMessage.data;

            // set command to Quintus state, so we can add state change listeners
            // where we need to react on commands
            Q.state.set(command, data);
        };
    }


    // add send message to Q so that it's easy available everywhere
    Q.sendMessage = function(message) {
        console.log("sending message: " + message);
        connection.send(message);
    }

    // generate some id
    var generateUUID = function () {
        var d = new Date().getTime();
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = (d + Math.random() * 16) % 16 | 0;
            d = Math.floor(d / 16);
            return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
        });
        return uuid;
    };

    var registerClient = function () {
        var command = {};
        command.command = WSC_REGISTER_CLIENT;
        command.data = {};
        var clientId = generateUUID();
        Q.state.set(CLIENTID, clientId);
        command.data[WSC_REGISTER_CLIENT_ID] = clientId;
        var commandString = JSON.stringify(command);
        Q.sendMessage(commandString);
    }

    connect();

    // called from html form
    joinRace = function(form){
        var name = form.name.value;
        if(name.trim().length == 0){
            alert("Please enter your name");
            return false;
        }
        else {
            var command = {};
            command.command = WSC_REGISTER_CAR;
            command.data = {};
            command.data[WSC_REGISTER_CAR_NAME] = name;
            var commandString = JSON.stringify(command);
            Q.sendMessage(commandString);
            return false;
        }
    }


};