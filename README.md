# MoneyDance Monthly Budget Editor

## WARNING! As of 1/1/2024 I am no longer maintaining this code. It is freely available for someone else to pick up and maintain.

For help using this extension see the [Wiki](https://github.com/jerrymjones/MonthlyBudgetEditor/wiki).

Monthly Budget Editor is an extension for the [Moneydance](https://moneydance.com/)
Personal Finance app to help you easily enter monthly budget amounts for a year to keep track of how 
well you are meeting your monthly spending goals. The extension includes the ability to initialize a new 
budget from the prior year's budget or actual spending as well as extensive pop up menu support to easily
enter or update values for each spending category and month.

Monthly Budget Editor is a companion to to my [Monthly Budget Report](https://github.com/jerrymjones/MonthlyBudgetReport) 
which correctly handles negative budget amounts. I have also created [Monthly Budget Bars](https://github.com/jerrymjones/MonthlyBudgetBars) 
another companion that creates a Summary Page widget which also works properly with negative budget amounts.

## Installation

1. Either [build](#build) the source code or [download](https://github.com/jerrymjones/MonthlyBudgetEditor/releases/latest) the latest release.

2. Follow [Moneydance's official documentation to install extensions](https://help.infinitekind.com/support/solutions/articles/80000682003-installing-extensions).  
   Use the `Add From File...` option to load the `budgeteditor.mxt` file.

3. **The extension has not yet been audited and signed by The Infinite Kind**, so you'll get a warning asking you if you really want to continue loading 
   the extension, click **Yes** to continue loading the extension.
   
4. You can now open the extension by going to **Extensions > Monthly Budget Editor**.

## Build

1. Clone the repository to your local system:

```shell
git clone https://github.com/jerrymjones/MonthlyBudgetEditor.git <localfolder>
```

2. Initialize the folder structure for building. The following command needs to be executed in `src/` i.e. `cd <localfolder>/src`:

```shell
ant init
```

3. Download the Moneydance [Developer's Kit](https://infinitekind.com/dev/moneydance-devkit-5.1.tar.gz) and extract it
   to a local folder on your system. Once extracted, copy-paste `lib/extadmin.jar` and `lib/moneydance-dev.jar` into the `<localfolder>/lib` folder:

```shell
cd tmp/
curl -O https://infinitekind.com/dev/moneydance-devkit-5.1.tar.gz
tar xzvf moneydance-devkit-5.1.tar.gz
cp moneydance-devkit-5.1/lib/* ... 
```

4. Generate a key pair (as required by Moneydance) to sign your locally built extension. You will be prompted for a passphrase that is used to
   encrypt the private key file. Your new keys will be stored in the priv_key and pub_key files. The command needs to be executed in `<localfolder>/src`:

```shell
ant genkeys
```

5. Build the extension from `<localfolder>/src`:

```shell
ant budgeteditor
```

6. Install the extension per the installation instructions [above](#installation) using `<localfolder>/dist/budgeteditor.mxt` as the file to load.
