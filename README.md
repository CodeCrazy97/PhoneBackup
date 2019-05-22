# SaveSMSMessages
Extracts message text from my Android SMS/MMS messages and places them in a MySQL database.

An example of an SMS message in XML format that this program would use is shown below....
  <sms protocol="0" address="(111) 123-4567" date="1476397734448" type="2" subject="null" body="Hi John! How are you?" toa="null" sc_toa="null" service_center="null" read="1" status="-1" locked="0" date_sent="0" readable_date="Oct 23, 2016 7:48:54 PM" contact_name="John" />
  
An example MMS message (MMS messages contain pictures or a very large amount of text) is below:
<mms text_only="0" ct_t="application/vnd.wap.multipart.related" msg_box="1" v="16" sub="null" seen="1" rr="null" ct_cls="null" retr_txt_cs="null" ct_l="null" phone_id="0" m_size="326222" exp="null" sub_cs="null" st="null" creator="null" tr_id="xxxx" sub_id="0" read="1" resp_st="null" date="1558068559000" m_id="xxxx" date_sent="0" pri="null" m_type="132" textlink="-1" address="10109988282" d_rpt="null" d_tm="null" read_status="null" locked="0" retr_txt="null" resp_txt="null" rpt_a="null" retr_st="null" m_cls="null" readable_date="May 11, 2019 10:49:19 AM" contact_name="John">
    <parts>
      <part seq="-1" ct="application/smil" name="0.smil" chset="106" cd="null" fn="null" cid="&lt;0.smil&gt;" cl="0.smil" ctt_s="null" ctt_t="null" text='&lt;smil&gt;&lt;head&gt;&lt;layout&gt;&lt;root-layout width="100%" height="100%"/&gt;&lt;region id="Image" width="100%" height="100%" top="0%" left="0%" fit="meet"/&gt;&lt;/layout&gt;&lt;/head&gt;&lt;body&gt;&lt;par dur="10000ms"&gt;&lt;img src="IMG950159.jpg" region="Image"/&gt;&lt;/par&gt;&lt;par dur="10000ms"&gt;&lt;img src="IMG950160.jpg" region="Image"/&gt;&lt;/par&gt;&lt;par dur="10000ms"&gt;&lt;img src="IMG950161.jpg" region="Image"/&gt;&lt;/par&gt;&lt;/body&gt;&lt;/smil&gt;' />
      <part seq="0" ct="image/jpeg" name="IMG950159.jpg" chset="null" cd="inline" fn="null" cid="&lt;IMG950159.jpg&gt;" cl="IMG950159.jpg" ctt_s="null" ctt_t="null" text="null" data="data" />
      <part seq="0" ct="image/jpeg" name="IMG950160.jpg" chset="null" cd="inline" fn="null" cid="&lt;IMG950160.jpg&gt;" cl="IMG950160.jpg" ctt_s="null" ctt_t="null" text="null" data="data" />
      <part seq="0" ct="image/jpeg" name="IMG950161.jpg" chset="null" cd="inline" fn="null" cid="&lt;IMG950161.jpg&gt;" cl="IMG950161.jpg" ctt_s="null" ctt_t="null" text="null" data="" />
    </parts>
    <addrs>
      <addr address="123456789" type="137" charset="106" />
      <addr address="098765431" type="151" charset="106" />
    </addrs>
  </mms>
  
  Finally, an example phone call:
  <call number="1123228372" duration="33" date="1494795178338" type="2" presentation="1" readable_date="Oct 14, 2017 8:52:58 PM" contact_name="Sally" />
