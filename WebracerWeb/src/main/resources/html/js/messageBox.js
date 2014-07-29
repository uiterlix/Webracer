;Quintus.WebracerMessageBox = function (Q) {

    Q.scene('messageBox', function (stage) {

        // always show the last 5 message
        // init empty messages
        var nrMessages = 5;
        var firstLine = "Messages from Race Control:                                            ";
        var messages = new Array();
        messages.push(firstLine);
        for(var i=0; i<nrMessages; i++){
            messages.push("");
        }

        var box = stage.insert(new Q.UI.Container({
            fill: "rgba(255,0,0,0.3)",
            border: 2,
            x: 550,
            y: 450,
            scale: 0.6
        }));

        var infotext = box.insert(
            new Q.UI.Text({x: 0, y: 0, align: "left", label: messages.join('\n')})
        );

        box.fit(10);

        updateMessageBox = function (data) {
            var message = data[WSS_MESSAGE_MESSAGE];
            var date = new Date();
            var hours = date.getHours();
            hours = hours < 10 ? "0" + hours : hours;
            var minutes = date.getMinutes();
            minutes = minutes < 10 ? "0" + minutes : minutes;
            var seconds = date.getSeconds();
            seconds = seconds < 10 ? "0" + seconds : seconds;
            var time = hours + ":" + minutes + ":" + seconds;

            // remove first 2 lines, re-add header line
            messages.shift();
            messages.shift();
            messages.unshift(firstLine);
            messages.push(time + " : " + message);

            infotext.p.label = messages.join("\n");
        }

        Q.state.on("change." + WSS_MESSAGE, updateMessageBox);
    });

};