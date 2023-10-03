class LogWidget {
  constructor(rootElement) {
    this.rootElement = rootElement;
    this.topicSelectorElement = $("<select>").appendTo(this.rootElement);
    this.rootElement.append("<br>");
    this.textElement =
        $("<textarea rows='25' cols='120' readonly>")
        .appendTo(this.rootElement);
    this.messages = [];
    this.topics = new Set();
  }

  addMessage(message) {
    if (message.id >= this.messages.length) {
      this.messages.push(message);
    } else {
      this.messages[message.id] = message;
    }
    this.textElement.val(this.messages.map(({text}) => text + '\n').join(''));
    this.scrollToBottom();
  }

  resetText() {
    this.messages = [];
    this.textElement.val('');
  }

  addTopic(topic) {
    if (this.topics.has(topic)) {
      return;
    }
    this.topics.add(topic);
    this.topicSelectorElement.append(
        `<option value="${topic}">${topic}</option>`);
  }

  resetTopics() {
    this.topics.clear();
    this.topicSelectorElement.empty();
  }

  onTopicSelect(callback) {
    this.topicSelectorElement.on("change", function () {
      callback(this.value);
    });
  }

  scrollToBottom() {
    this.textElement.scrollTop = this.textElement.scrollHeight;
  }

  getLastSeenMessageId() {
    return this.messages.length - 1;
  }
}

class LogRetriever {
  constructor(endpoint) {
    this.endpoint = endpoint;
  }

  async topics() {
    return new Promise((resolve, reject) => {
      $.ajax({
        dataType: "json",
        url: this.endpoint,
        cache: false,
        type: "GET",
        success: resolve,
        error: reject
      });
    });
  }

  async retrieve(topic, lastSeen) {
    return new Promise((resolve, reject) => {
      $.ajax({
        dataType: "json",
        url: this.endpoint + "/" + topic + "?lastSeen=" + lastSeen,
        data: {},
        cache: false,
        type: "GET",
        success: resolve,
        error: reject
      });
    });
  }
}

class LogManager {
  constructor(widget, retriever) {
    this.widget = widget;
    this.retriever = retriever;
    this.epoch = 0;
    this.selectedTopic = null;

    this.widget.onTopicSelect(newTopic => this.setTopic(newTopic));

    this.pingInterval = setInterval(async () => this.retrieveLogsAndTopics(),
        1000);
  }

  release() {
    clearInterval(this.pingInterval);
  }

  setTopic(topic) {
    this.selectedTopic = topic;
    ++this.epoch;
    this.widget.resetText();
    this.retrieveLogs();
  }

  async retrieveLogsAndTopics() {
    await Promise.all([
      this.retrieveLogs(),
      this.retrieveTopics()
    ]);
  }

  async retrieveTopics() {
    const newTopics = new Set(await this.retriever.topics());

    for (const oldTopic of this.widget.topics) {
      if (!newTopics.has(oldTopic)) {
        this.widget.resetTopics();
        this.widget.resetText();
        break;
      }
    }

    for (const topic of newTopics) {
      if (this.selectedTopic == null) {
        this.setTopic(topic);
      }
      this.widget.addTopic(topic);
    }
  }

  async retrieveLogs() {

    let messages = [];
    do {
      messages = await this._doRetrieve();

      for (const message of messages) {
        this.widget.addMessage(message);
      }
    } while (messages.length > 0);
  }

  async _doRetrieve() {
    if (this.selectedTopic == null) {
      return [];
    }
    const beforeEpoch = this.epoch;
    const result = await this.retriever.retrieve(
        this.selectedTopic,
        this.widget.getLastSeenMessageId());
    const afterEpoch = this.epoch;
    if (beforeEpoch !== afterEpoch) {
      return [];
    }
    return result;
  }
}

$(document).ready(() => {
  console.log('Document is ready');

  window.logManager = new LogManager(new LogWidget($("#logger-div")),
      new LogRetriever("/api/logs"));
  window.logManager.retrieveLogsAndTopics();
});

function oktaLogin() {
  $.post("/api/drivers/okta_driver/login", {
    username: document.getElementById("okta-username").value,
    password: document.getElementById("okta-password").value,
  })
}

function googleLogin() {
  $.post("/api/drivers/google_login_driver/login", {
    username: document.getElementById("google-username").value,
    password: document.getElementById("google-password").value,
  })
}