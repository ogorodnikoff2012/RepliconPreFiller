Replicon PreFiller
===

I'm too lazy to write a good readme. Normally, you run `tk.xenon98.replicon.RepliconPreFillerApplication.main()`,
enter your credentials (they're not sent to my secret database, I swear!) and enjoy your replicon being filled with
data. Also you need to copy `src/main/resources/post-checkout` to your `.git/hooks` directory and set up path to
git repo in `src/main/resources/application.properties`.