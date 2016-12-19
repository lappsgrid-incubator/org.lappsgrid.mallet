## org.lappsgrid.mallet
This repository contains NLP tools from UMASS Amherst. There are in total 6 tools. The tools are a document classifier, a sequence tagger, and a topic modeler plus their respective trainers. 

# Input
Each tool except the Sequence Tagger requires a LAPPS Grid Data object with the discriminator: [Discriminators.Uri.TEXT](http://vocab.lappsgrid.org/ns/media/text) and the text in the payload. The text in the payload for the non-trainer tools is the text we wish to be analyzed. The text in the payload for the trainer tools is not used so it can be ```null```. The Sequence Tagger requires [Discriminators.Uri.TOKEN](http://vocab.lappsgrid.org/Token) as the discriminator and appropriate tokens in the Data object.

##Parameters
Each tool will also require a few parameters.

###Document Classifier
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|classifier| The path to the classifier model | "/masc_500k_texts.classifier" |

###Sequence Tagger
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|model| The path to the sequence tagger model | "/masc_500k_texts.model" |

###Topic Modeler
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|inferencer| The path to the topic modeler model | "/masc_500k_texts.inferencer" |
|keys| The path to the topic keys file | "/masc_500k_texts_topic_keys.txt"|
|numIterations| Number of times to sample | 100|
|burnIn| Percentage burn-in | 10 |
|thinning| The thinning interval | 10 |

###Document Classifier Trainer
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|directory| The directory in which the .txt files used for training are held | null |
|path| The directory in which the model will be written | null|
|classifierName| The name of the classifier file (e.g. "masc_500k_texts.classifier") to be written | null
|trainer| The type of train to be used. The choices are "NaiveBayes", "MaxEnt", "BalancedWinnow", "C45", "DecisionTree", "MaxEntL1", "MCMaxEnt", "NaiveBayesEMT", "Winnow". | "NaiveBayes"|

###Sequence Tagger Trainer
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|directory| The directory in which the .txt files used for training are held | null |
|path| The directory in which the model will be written | null|
|modelName| The name of the sequence tagger model file (e.g. "masc_500k_texts.model") to be written | null

###Topic Modeler Trainer
|Parameter Name|Description|Default Value|
| --- | --- | --- |
|directory| The directory in which the .txt files used for training are held | null |
|path| The directory in which the model will be written | null|
|inferencerName| The name of the topic modeler inferencer file (e.g. "masc_500k_texts.inferencer") to be written | null
|keysName | The name of the topic modeler topic keys file (e.g. "/masc_500k_texts_topic_keys.txt") to be written | null
|numTopics| The number of topics (group of similar words) to be created | null|
|wordsPerTopic| The number of words in each topic | null|

#Output

All trainers will return ``{"discriminator":"http://vocab.lappsgrid.org/ns/media/text","payload":"Success"}`` if trainer was successful.

##Document Classifier
The document classifier will return Data with a [JSON](http://vocab.lappsgrid.org/ns/media/json) discriminator. The output will be annotations for each document type the classifier was trained for and the probability that the text in our payload is of that document type.

<details>
<summary>Example Output</summary>
```json
{
  "discriminator" : "http://vocab.lappsgrid.org/ns/media/json",
  "payload" : {
    "@context" : "http://vocab.lappsgrid.org/context-1.0.0.jsonld",
    "metadata" : { },
    "text" : {
      "@value" : "When you access Basic Completion by pressing Ctrl+Space, you get basic suggestions for variables, types, methods, expressions, and so on. When you call Basic Completion twice, it shows you more results, including private members and non-imported static members.\nThe Smart Completion feature is aware of the expected type and data flow, and offers the options relevant to the context. To call Smart Completion, press Ctrl+Shift+Space. When you call Smart Completion twice, it shows you more results, including chains.\nTo overwrite the identifier at the caret, instead of just inserting the suggestion, press Tab. This is helpful if you're editing part of an identifier, such as a file name.\nTo let IntelliJ IDEA complete a statement for you, press Ctrl+Shift+Enter. Statement Completion will automatically add the missing parentheses, brackets, braces and the necessary formatting.\nIf you want to see the suggested parameters for any method or constructor, press Ctrl+P. IntelliJ IDEA shows the parameter info for each overloaded method or constructor, and highlights the best match for the parameters already typed.\nThe Postfix Completion feature lets you transform an already typed expression to another one based on the postfix you type after a period, the expression type, and its context."
    },
    "views" : [ {
      "metadata" : {
        "contains" : {
          "document types" : {
            "producer" : "org.lappsgrid.mallet.DocumentClassification",
            "type" : "document-types:mallet"
          }
        }
      },
      "annotations" : [ {
        "id" : "documentType0",
        "features" : {
          "documentType" : "email",
          "probability" : "0.16769196908503556"
        }
      }, {
        "id" : "documentType1",
        "features" : {
          "documentType" : "newspaper_newswire",
          "probability" : "0.15269109839819786"
        }
      }, {
        "id" : "documentType2",
        "features" : {
          "documentType" : "letters",
          "probability" : "0.10096031267887731"
        }
      }, {
        "id" : "documentType3",
        "features" : {
          "documentType" : "spam",
          "probability" : "0.08455659858471774"
        }
      }, {
        "id" : "documentType4",
        "features" : {
          "documentType" : "journal",
          "probability" : "0.04551115427282711"
        }
      }, {
        "id" : "documentType5",
        "features" : {
          "documentType" : "movie-script",
          "probability" : "0.0380435195433462"
        }
      }, {
        "id" : "documentType6",
        "features" : {
          "documentType" : "jokes",
          "probability" : "0.03579415539387567"
        }
      }, {
        "id" : "documentType7",
        "features" : {
          "documentType" : "essays",
          "probability" : "0.0317846996088732"
        }
      }, {
        "id" : "documentType8",
        "features" : {
          "documentType" : "technical",
          "probability" : "0.031520598294279896"
        }
      }, {
        "id" : "documentType9",
        "features" : {
          "documentType" : "non-fiction",
          "probability" : "0.03081788850443121"
        }
      }, {
        "id" : "documentType10",
        "features" : {
          "documentType" : "blog",
          "probability" : "0.030392432805804375"
        }
      }, {
        "id" : "documentType11",
        "features" : {
          "documentType" : "fiction",
          "probability" : "0.029407520550799326"
        }
      }, {
        "id" : "documentType12",
        "features" : {
          "documentType" : "govt-docs",
          "probability" : "0.029032915493799397"
        }
      }, {
        "id" : "documentType13",
        "features" : {
          "documentType" : "court transcript",
          "probability" : "0.028816569544770857"
        }
      }, {
        "id" : "documentType14",
        "features" : {
          "documentType" : "travel-guides",
          "probability" : "0.0279566731838214"
        }
      }, {
        "id" : "documentType15",
        "features" : {
          "documentType" : "twitter",
          "probability" : "0.027532100906652333"
        }
      }, {
        "id" : "documentType16",
        "features" : {
          "documentType" : "face-to-face",
          "probability" : "0.027295018793301956"
        }
      }, {
        "id" : "documentType17",
        "features" : {
          "documentType" : "ficlets",
          "probability" : "0.027127692885481405"
        }
      }, {
        "id" : "documentType18",
        "features" : {
          "documentType" : "telephone",
          "probability" : "0.026654428854579585"
        }
      }, {
        "id" : "documentType19",
        "features" : {
          "documentType" : "debate-transcript",
          "probability" : "0.026412652616527484"
        }
      } ]
    } ]
  },
  "parameters" : {
    "model" : "PATH/TO/CLASSIFIER/masc_500k_texts.classifier"
  }
}
```
</details>

##Sequence Tagger
The Sequence Tagger will produce annotations with [part of speech tokens](http://vocab.lappsgrid.org/Token#pos) along with their start and end positions.

<details>
<summary>Example Output</summary>
````json
{
  "discriminator" : "http://vocab.lappsgrid.org/ns/media/jsonld#lif",
  "payload" : {
    "@context" : "http://vocab.lappsgrid.org/context-1.0.0.jsonld",
    "metadata" : { },
    "text" : { },
    "views" : [ {
      "metadata" : {
        "contains" : {
          "http://vocab.lappsgrid.org/Token#pos" : {
            "producer" : "org.lappsgrid.mallet.SequenceTagging",
            "type" : "pos:mallet"
          }
        }
      },
      "annotations" : [ {
        "id" : "tok0",
        "start" : 0,
        "end" : 2,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "Don",
          "pos" : "NNP"
        }
      }, {
        "id" : "tok1",
        "start" : 3,
        "end" : 4,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "'t",
          "pos" : ","
        }
      }, {
        "id" : "tok2",
        "start" : 6,
        "end" : 10,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "count",
          "pos" : "VBZ"
        }
      }, {
        "id" : "tok3",
        "start" : 12,
        "end" : 14,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "the",
          "pos" : "DT"
        }
      }, {
        "id" : "tok4",
        "start" : 16,
        "end" : 19,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "days",
          "pos" : "NNP"
        }
      }, {
        "id" : "tok5",
        "start" : 20,
        "end" : 20,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : ".",
          "pos" : "."
        }
      }, {
        "id" : "tok6",
        "start" : 22,
        "end" : 25,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "Make",
          "pos" : "VBZ"
        }
      }, {
        "id" : "tok7",
        "start" : 27,
        "end" : 29,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "the",
          "pos" : "DT"
        }
      }, {
        "id" : "tok8",
        "start" : 31,
        "end" : 34,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "days",
          "pos" : "NNP"
        }
      }, {
        "id" : "tok9",
        "start" : 36,
        "end" : 40,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : "count",
          "pos" : "NNP"
        }
      }, {
        "id" : "tok10",
        "start" : 41,
        "end" : 41,
        "@type" : "http://vocab.lappsgrid.org/Token#pos",
        "features" : {
          "word" : ".",
          "pos" : "."
        }
      } ]
    } ]
  },
  "parameters" : { }
}
````
</details>

##Topic Modeler
The Topic Modeler produces annotations for each topic the inferencer file was trained for, along with the proportion of our text in the payload that pertains to that topic.

<details>
<summary>Example Output</summary>
````json
{
  "discriminator" : "http://vocab.lappsgrid.org/ns/media/json",
  "payload" : {
    "@context" : "http://vocab.lappsgrid.org/context-1.0.0.jsonld",
    "metadata" : { },
    "text" : {
      "@value" : "Research scientists are the primary audience for the journal, but summaries and accompanying articles are intended to make many of the most important papers understandable to scientists in other fields and the educated public. Towards the front of each issue are editorials, news and feature articles on issues of general interest to scientists, including current affairs, science funding, business, scientific ethics and research breakthroughs. There are also sections on books and arts. The remainder of the journal consists mostly of research papers (articles or letters), which are often dense and highly technical. Because of strict limits on the length of papers, often the printed text is actually a summary of the work in question with many details relegated to accompanying supplementary material on the journal's website."
    },
    "views" : [ {
      "metadata" : {
        "contains" : {
          "topic proportions" : {
            "producer" : "org.lappsgrid.mallet.TopicModeling",
            "type" : "topic-proportions:mallet"
          }
        }
      },
      "annotations" : [ {
        "id" : "topic0",
        "features" : {
          "topic" : "wildlife today year community work goodwill people youth animals children dear job animal special guide young sincerely family ",
          "proportion" : "0.0035211267605633804"
        }
      }, {
        "id" : "topic1",
        "features" : {
          "topic" : "nest pattern expression gad antie species function ants river gradient valley end channels fibroblasts lakes change scd bound ",
          "proportion" : "0.12089201877934272"
        }
      }, {
        "id" : "topic2",
        "features" : {
          "topic" : "city chinese vegas las road island east street center built china kong’s macau town bay museum market temple ",
          "proportion" : "0.008215962441314555"
        }
      }, {
        "id" : "topic3",
        "features" : {
          "topic" : "eyes looked back door face nepthys room don’t didn’t air hands walked hair turned hand girl dark it’s ",
          "proportion" : "0.128716744913928"
        }
      }, {
        "id" : "topic4",
        "features" : {
          "topic" : "america states world state law american make government country united health important money national tax care work county ",
          "proportion" : "0.15062597809076683"
        }
      }, {
        "id" : "topic5",
        "features" : {
          "topic" : "indigenous document words languages book english microsoft identity native word spanish org chapter disease tom women patients tests ",
          "proportion" : "0.016040688575899843"
        }
      }, {
        "id" : "topic6",
        "features" : {
          "topic" : "day good long make life people home made years find end put give left great times found place ",
          "proportion" : "0.005086071987480438"
        }
      }, {
        "id" : "topic7",
        "features" : {
          "topic" : "power significant crisis patients results system yhl emissions risk study important health energy normal yol markets program effect ",
          "proportion" : "0.022300469483568074"
        }
      }, {
        "id" : "topic8",
        "features" : {
          "topic" : "mail commission moore rate services report rules proposed karnes classification seaman request interview act change tibbets heston decision ",
          "proportion" : "0.020735524256651018"
        }
      }, {
        "id" : "topic9",
        "features" : {
          "topic" : "that's yeah i'm i've we're people there's they're things doesn't you're didn't make lot question good o_k can't ",
          "proportion" : "0.005086071987480438"
        }
      }, {
        "id" : "topic10",
        "features" : {
          "topic" : "form movement fact career section system measures industry process autonomous agents structure clear theme university simple people point ",
          "proportion" : "0.0035211267605633804"
        }
      }, {
        "id" : "topic11",
        "features" : {
          "topic" : "court congress years argument building architecture clause buildings public jerusalem term works modern justice time jewish land greek ",
          "proportion" : "0.19757433489827855"
        }
      }, {
        "id" : "topic12",
        "features" : {
          "topic" : "email charset message xxx@xxx.xxx enron account bit character information contact vince tue money us-ascii text/plain content-type philip content-transfer-encoding ",
          "proportion" : "0.0035211267605633804"
        }
      }, {
        "id" : "topic13",
        "features" : {
          "topic" : "black man young remember thorndike mind allan stories tasha andrews space bike men ride colors true chair movie ",
          "proportion" : "0.04107981220657277"
        }
      }, {
        "id" : "topic14",
        "features" : {
          "topic" : "i'm watching work day http://bit.ly working blog reading post news today sleep posted cool video back web love ",
          "proportion" : "0.005086071987480438"
        }
      }, {
        "id" : "topic15",
        "features" : {
          "topic" : "time read meeting don't i'm correct question article letter understanding that's book case remember students statement biology honor ",
          "proportion" : "0.05516431924882629"
        }
      }, {
        "id" : "topic16",
        "features" : {
          "topic" : "president iraq war states bush told senate september secretary attacks team terrorism russian government plans military chang official ",
          "proportion" : "0.18035993740219092"
        }
      }, {
        "id" : "topic17",
        "features" : {
          "topic" : "elizabeth turner gibbs jones swann norrington pintel davy beckett black ragetti captain crew ship key chest pearl lord ",
          "proportion" : "0.023865414710485134"
        }
      }, {
        "id" : "topic18",
        "features" : {
          "topic" : "title man grant clark jeffery alan malcolm ian sanchez can't asked god you're it's i'm don't gen didn't ",
          "proportion" : "0.005086071987480438"
        }
      }, {
        "id" : "topic19",
        "features" : {
          "topic" : "company business market share credit card access bank offer year price california costs lsc stock shares management payment ",
          "proportion" : "0.0035211267605633804"
        }
      } ]
    } ]
  },
  "parameters" : {
    "inferencer" : "PATH/TO/INFERENCER/masc_500k_texts.inferencer",
    "keys" : "PATH/TO/INFERENCERKEYS/masc_500k_texts_topic_keys.txt",
    "numIterations" : 100,
    "thinning" : 10,
    "burnIn" : 10
  }
}
````
</details>
